"""Kafka producer service for publishing ecommerce events"""

import json
import time
import random
import threading
import logging
from typing import Optional
from kafka import KafkaProducer
from kafka.errors import KafkaError

from .config import settings
from .events import EventGenerator
from .models import EcommerceEvent


logger = logging.getLogger(__name__)


class KafkaProducerService:
    """Thread-safe Kafka producer service"""
    
    def __init__(self):
        self.producer: Optional[KafkaProducer] = None
        self.event_generator = EventGenerator()
        self.running = False
        self.producer_thread: Optional[threading.Thread] = None
        self.events_published = 0
        self.lock = threading.Lock()
    
    def _create_producer(self) -> KafkaProducer:
        """Create and return a Kafka producer instance"""
        try:
            producer = KafkaProducer(
                bootstrap_servers=settings.kafka_bootstrap_servers,
                value_serializer=lambda v: json.dumps(v, default=str).encode('utf-8'),
                key_serializer=lambda k: k.encode('utf-8') if k else None,
                acks='all',  # Wait for all replicas to acknowledge
                retries=3,
                max_in_flight_requests_per_connection=1,
                enable_idempotence=True
            )
            logger.info(f"Kafka producer created. Connected to {settings.kafka_bootstrap_servers}")
            return producer
        except KafkaError as e:
            logger.error(f"Failed to create Kafka producer: {e}")
            raise
    
    def start(self) -> bool:
        """Start the producer service"""
        with self.lock:
            if self.running:
                logger.warning("Producer is already running")
                return False
            
            try:
                self.producer = self._create_producer()
                self.running = True
                self.producer_thread = threading.Thread(target=self._producer_loop, daemon=True)
                self.producer_thread.start()
                logger.info("Producer service started")
                return True
            except Exception as e:
                logger.error(f"Failed to start producer: {e}")
                self.running = False
                return False
    
    def stop(self):
        """Stop the producer service"""
        with self.lock:
            if not self.running:
                return
            
            self.running = False
            
            if self.producer_thread and self.producer_thread.is_alive():
                self.producer_thread.join(timeout=5.0)
            
            if self.producer:
                try:
                    self.producer.flush(timeout=10.0)
                    self.producer.close()
                    logger.info("Producer service stopped")
                except Exception as e:
                    logger.error(f"Error closing producer: {e}")
                finally:
                    self.producer = None
    
    def _producer_loop(self):
        """Main producer loop that generates and publishes events"""
        logger.info("Producer loop started")
        
        while self.running:
            try:
                # Generate event
                event = self.event_generator.generate_event()
                
                # Publish to Kafka
                self.publish_event(event)
                
                # Wait between events (simulate real-world traffic)
                time.sleep(random.uniform(0.1, 1.0))
            
            except Exception as e:
                logger.error(f"Error in producer loop: {e}")
                if self.running:
                    time.sleep(1.0)  # Wait before retrying
    
    def publish_event(self, event: EcommerceEvent) -> bool:
        """Publish a single event to Kafka"""
        if not self.producer:
            logger.error("Producer not initialized")
            return False
        
        try:
            # Use user_id as key for partitioning
            key = event.user.user_id
            
            # Convert event to dict
            event_dict = event.model_dump()
            
            # Send to Kafka
            future = self.producer.send(
                settings.kafka_topic,
                key=key,
                value=event_dict
            )
            
            # Handle callback
            def on_send_success(record_metadata):
                with self.lock:
                    self.events_published += 1
                logger.debug(
                    f"Event published: topic={record_metadata.topic}, "
                    f"partition={record_metadata.partition}, "
                    f"offset={record_metadata.offset}"
                )
            
            def on_send_error(exception):
                logger.error(f"Error publishing event: {exception}")
            
            future.add_callback(on_send_success)
            future.add_errback(on_send_error)
            
            return True
        
        except Exception as e:
            logger.error(f"Failed to publish event: {e}")
            return False
    
    def get_status(self) -> dict:
        """Get producer status"""
        with self.lock:
            return {
                'running': self.running,
                'events_published': self.events_published,
                'connected': self.producer is not None
            }
