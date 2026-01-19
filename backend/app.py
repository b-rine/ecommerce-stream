"""FastAPI web backend with WebSocket support"""

import asyncio
import logging
from contextlib import asynccontextmanager
from typing import List
from fastapi import FastAPI, WebSocket, WebSocketDisconnect, HTTPException
from fastapi.staticfiles import StaticFiles
from fastapi.responses import HTMLResponse
import uvicorn

from .config import settings
from .kafka_producer import KafkaProducerService
from .kafka_consumer import KafkaConsumerService
from .models import Statistics, StreamStatus, EcommerceEvent


# Configure logging
logging.basicConfig(
    level=getattr(logging, settings.log_level.upper()),
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler(settings.log_file),
        logging.StreamHandler()
    ]
)
logger = logging.getLogger(__name__)

# Global services
producer_service: KafkaProducerService = None
consumer_service: KafkaConsumerService = None
active_websockets: List[WebSocket] = []


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Lifespan context manager for startup and shutdown"""
    global producer_service, consumer_service
    
    # Startup
    logger.info("Starting application...")
    producer_service = KafkaProducerService()
    consumer_service = KafkaConsumerService()
    
    # Start background task for broadcasting events
    asyncio.create_task(broadcast_events_task())
    
    yield
    
    # Shutdown
    logger.info("Shutting down application...")
    if producer_service:
        producer_service.stop()
    if consumer_service:
        consumer_service.stop()


app = FastAPI(
    title="Ecommerce Stream API",
    description="Real-time ecommerce event streaming with Kafka",
    version="1.0.0",
    lifespan=lifespan
)

# Mount static files (frontend)
import os
frontend_path = os.path.join(os.path.dirname(os.path.dirname(__file__)), "frontend")
if os.path.exists(frontend_path):
    app.mount("/static", StaticFiles(directory=frontend_path), name="static")
else:
    logger.warning(f"Frontend directory not found at {frontend_path}, static files will not be served")


async def broadcast_events_task():
    """Background task to broadcast events to WebSocket clients"""
    while True:
        try:
            if consumer_service and consumer_service.running:
                # Get latest events
                events = consumer_service.get_latest_events(limit=10)
                
                if events and active_websockets:
                    # Convert events to dict
                    events_data = [event.model_dump() for event in events]
                    
                    # Broadcast to all connected clients
                    disconnected = []
                    for websocket in active_websockets:
                        try:
                            await websocket.send_json({
                                'type': 'events',
                                'data': events_data
                            })
                        except Exception as e:
                            logger.debug(f"Error sending to websocket: {e}")
                            disconnected.append(websocket)
                    
                    # Remove disconnected websockets
                    for ws in disconnected:
                        if ws in active_websockets:
                            active_websockets.remove(ws)
                
                # Broadcast statistics update
                if consumer_service.running and active_websockets:
                    stats = consumer_service.get_statistics()
                    stats_data = stats.model_dump()
                    
                    disconnected = []
                    for websocket in active_websockets:
                        try:
                            await websocket.send_json({
                                'type': 'stats',
                                'data': stats_data
                            })
                        except Exception as e:
                            logger.debug(f"Error sending stats to websocket: {e}")
                            disconnected.append(websocket)
                    
                    for ws in disconnected:
                        if ws in active_websockets:
                            active_websockets.remove(ws)
            
            await asyncio.sleep(1.0)  # Update every second
        
        except Exception as e:
            logger.error(f"Error in broadcast task: {e}")
            await asyncio.sleep(1.0)


@app.get("/", response_class=HTMLResponse)
async def read_root():
    """Serve the main dashboard page"""
    import os
    index_path = os.path.join(os.path.dirname(os.path.dirname(__file__)), "frontend", "index.html")
    try:
        with open(index_path, "r") as f:
            return HTMLResponse(content=f.read())
    except FileNotFoundError:
        return HTMLResponse(
            content=f"<h1>Frontend not found</h1><p>Please ensure frontend/index.html exists at {index_path}</p>",
            status_code=404
        )


@app.websocket("/ws")
async def websocket_endpoint(websocket: WebSocket):
    """WebSocket endpoint for real-time updates"""
    await websocket.accept()
    active_websockets.append(websocket)
    logger.info(f"WebSocket client connected. Total connections: {len(active_websockets)}")
    
    try:
        # Send initial statistics
        if consumer_service:
            stats = consumer_service.get_statistics()
            await websocket.send_json({
                'type': 'stats',
                'data': stats.model_dump()
            })
        
        # Keep connection alive and handle incoming messages
        while True:
            try:
                data = await websocket.receive_json()
                # Handle client messages if needed
                logger.debug(f"Received WebSocket message: {data}")
            except WebSocketDisconnect:
                break
            except Exception as e:
                logger.error(f"WebSocket error: {e}")
                break
    
    except WebSocketDisconnect:
        pass
    finally:
        if websocket in active_websockets:
            active_websockets.remove(websocket)
        logger.info(f"WebSocket client disconnected. Total connections: {len(active_websockets)}")


@app.get("/api/stats")
async def get_statistics() -> Statistics:
    """Get current statistics"""
    if not consumer_service:
        raise HTTPException(status_code=503, detail="Consumer service not initialized")
    
    return consumer_service.get_statistics()


@app.get("/api/stream/status")
async def get_stream_status() -> StreamStatus:
    """Get stream status"""
    producer_status = producer_service.get_status() if producer_service else {'running': False, 'events_published': 0}
    consumer_status = consumer_service.get_status() if consumer_service else {'running': False, 'events_processed': 0}
    
    return StreamStatus(
        producer_running=producer_status.get('running', False),
        consumer_running=consumer_status.get('running', False),
        events_published=producer_status.get('events_published', 0),
        events_processed=consumer_status.get('events_processed', 0)
    )


@app.post("/api/stream/start")
async def start_stream():
    """Start the producer and consumer"""
    if not producer_service or not consumer_service:
        raise HTTPException(status_code=503, detail="Services not initialized")
    
    producer_started = producer_service.start()
    consumer_started = consumer_service.start()
    
    if producer_started and consumer_started:
        return {"message": "Stream started successfully", "producer": True, "consumer": True}
    elif producer_started:
        return {"message": "Producer started, consumer failed", "producer": True, "consumer": False}
    elif consumer_started:
        return {"message": "Consumer started, producer failed", "producer": False, "consumer": True}
    else:
        raise HTTPException(status_code=500, detail="Failed to start stream")


@app.post("/api/stream/stop")
async def stop_stream():
    """Stop the producer and consumer"""
    if producer_service:
        producer_service.stop()
    if consumer_service:
        consumer_service.stop()
    
    return {"message": "Stream stopped successfully"}


def main():
    """Main entry point"""
    uvicorn.run(
        "backend.app:app",
        host=settings.server_host,
        port=settings.server_port,
        reload=settings.environment == "development",
        log_level=settings.log_level.lower()
    )


if __name__ == "__main__":
    main()
