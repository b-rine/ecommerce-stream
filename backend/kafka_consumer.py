"""Kafka consumer service for processing ecommerce events"""

import json
import threading
import logging
import queue
import time
from typing import Optional, List, Set
from datetime import datetime
from kafka import KafkaConsumer
from kafka.errors import KafkaError

from .config import settings
from .models import EcommerceEvent, Statistics, EventType


logger = logging.getLogger(__name__)


class KafkaConsumerService:
    """Thread-safe Kafka consumer service with statistics aggregation"""
    
    def __init__(self, event_queue: Optional[queue.Queue] = None):
        self.consumer: Optional[KafkaConsumer] = None
        self.event_queue = event_queue or queue.Queue(maxsize=1000)
        self.running = False
        self.consumer_thread: Optional[threading.Thread] = None
        self.lock = threading.Lock()
        
        # Statistics
        self.stats = Statistics()
        self.users_seen: Set[str] = set()
        self.products_seen: Set[str] = set()
    
    def _create_consumer(self) -> KafkaConsumer:
        """Create and return a Kafka consumer instance"""
        try:
            consumer = KafkaConsumer(
                settings.kafka_topic,
                bootstrap_servers=settings.kafka_bootstrap_servers,
                group_id=settings.consumer_group_id,
                value_deserializer=lambda m: json.loads(m.decode('utf-8')),
                key_deserializer=lambda k: k.decode('utf-8') if k else None,
                auto_offset_reset='earliest',
                enable_auto_commit=True,
                consumer_timeout_ms=1000  # Timeout after 1 second of no messages
            )
            logger.info(
                f"Kafka consumer created. Subscribed to topic: {settings.kafka_topic}, "
                f"group: {settings.consumer_group_id}"
            )
            return consumer
        except KafkaError as e:
            logger.error(f"Failed to create Kafka consumer: {e}")
            raise
    
    def start(self) -> bool:
        """Start the consumer service"""
        with self.lock:
            if self.running:
                logger.warning("Consumer is already running")
                return False
            
            try:
                self.consumer = self._create_consumer()
                self.running = True
                self.consumer_thread = threading.Thread(target=self._consumer_loop, daemon=True)
                self.consumer_thread.start()
                logger.info("Consumer service started")
                return True
            except Exception as e:
                logger.error(f"Failed to start consumer: {e}")
                self.running = False
                return False
    
    def stop(self):
        """Stop the consumer service"""
        with self.lock:
            if not self.running:
                return
            
            self.running = False
            
            if self.consumer_thread and self.consumer_thread.is_alive():
                self.consumer_thread.join(timeout=5.0)
            
            if self.consumer:
                try:
                    self.consumer.close()
                    logger.info("Consumer service stopped")
                except Exception as e:
                    logger.error(f"Error closing consumer: {e}")
                finally:
                    self.consumer = None
    
    def _consumer_loop(self):
        """Main consumer loop that processes events"""
        logger.info("Consumer loop started")
        
        while self.running:
            try:
                # Poll for messages
                message_pack = self.consumer.poll(timeout_ms=1000)
                
                if not message_pack:
                    continue
                
                # Process messages
                for topic_partition, messages in message_pack.items():
                    for message in messages:
                        try:
                            event_data = message.value
                            event = EcommerceEvent(**event_data)
                            self._process_event(event)
                            
                            # Add to queue for WebSocket broadcasting
                            try:
                                self.event_queue.put_nowait(event)
                            except queue.Full:
                                # Queue is full, remove oldest and add new
                                try:
                                    self.event_queue.get_nowait()
                                    self.event_queue.put_nowait(event)
                                except queue.Empty:
                                    pass
                        
                        except Exception as e:
                            logger.error(f"Error processing message: {e}")
            
            except Exception as e:
                if self.running:
                    logger.error(f"Error in consumer loop: {e}")
                    time.sleep(1.0)  # Wait before retrying
    
    def _process_event(self, event: EcommerceEvent):
        """Process an event and update statistics"""
        with self.lock:
            # Update basic statistics
            self.stats.total_events += 1
            event_type_str = event.event_type.value if hasattr(event.event_type, 'value') else str(event.event_type)
            self.stats.events_by_type[event_type_str] = self.stats.events_by_type.get(event_type_str, 0) + 1
            
            # Track users
            user_id = event.user.user_id
            if user_id:
                self.users_seen.add(user_id)
                self.stats.unique_users = len(self.users_seen)
            
            # Track products
            if event.product:
                product_id = event.product.product_id
                if product_id:
                    self.products_seen.add(product_id)
                    self.stats.unique_products = len(self.products_seen)
            
            # Process purchase events
            if event.event_type == EventType.PURCHASE:
                self.stats.total_orders += 1
                if event.total_amount:
                    self.stats.total_revenue += event.total_amount
                    
                    # Track revenue over time
                    timestamp = datetime.utcnow().isoformat()
                    self.stats.revenue_over_time.append({
                        'timestamp': timestamp,
                        'revenue': event.total_amount
                    })
                    # Keep only last 100 data points
                    if len(self.stats.revenue_over_time) > 100:
                        self.stats.revenue_over_time.pop(0)
                    
                    # Track category sales
                    if event.product and event.product.category:
                        category = event.product.category
                        self.stats.category_sales[category] = \
                            self.stats.category_sales.get(category, 0.0) + event.total_amount
            
            # Track events per minute
            current_minute = datetime.utcnow().strftime('%Y-%m-%d %H:%M')
            if not self.stats.events_per_minute or \
               self.stats.events_per_minute[-1]['timestamp'] != current_minute:
                self.stats.events_per_minute.append({
                    'timestamp': current_minute,
                    'count': 1
                })
            else:
                self.stats.events_per_minute[-1]['count'] += 1
            
            # Keep only last 60 minutes
            if len(self.stats.events_per_minute) > 60:
                self.stats.events_per_minute.pop(0)
    
    def get_statistics(self) -> Statistics:
        """Get current statistics"""
        with self.lock:
            return self.stats.model_copy(deep=True)
    
    def get_latest_events(self, limit: int = 10) -> List[EcommerceEvent]:
        """Get latest events from queue"""
        events = []
        temp_queue = queue.Queue()
        
        # Drain queue and collect events
        while not self.event_queue.empty():
            try:
                event = self.event_queue.get_nowait()
                events.append(event)
                temp_queue.put(event)
            except queue.Empty:
                break
        
        # Put events back
        while not temp_queue.empty():
            self.event_queue.put(temp_queue.get())
        
        # Return most recent events
        return events[-limit:] if len(events) > limit else events
    
    def get_status(self) -> dict:
        """Get consumer status"""
        with self.lock:
            return {
                'running': self.running,
                'events_processed': self.stats.total_events,
                'connected': self.consumer is not None
            }
