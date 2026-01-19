"""Configuration management using environment variables"""

from pathlib import Path
from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    """Application settings loaded from environment variables"""
    
    # Kafka Configuration
    kafka_bootstrap_servers: str = "localhost:9092"
    kafka_topic: str = "ecommerce-events"
    consumer_group_id: str = "ecommerce-consumer-group"
    
    # Server Configuration
    server_port: int = 8000
    server_host: str = "0.0.0.0"
    
    # Logging Configuration
    log_level: str = "INFO"
    log_file: str = "ecommerce_stream.log"
    
    # Environment
    environment: str = "development"
    
    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"
        case_sensitive = False


# Global settings instance
settings = Settings()

# Ensure log directory exists
log_file_path = Path(settings.log_file)
if log_file_path.parent != Path("."):
    log_file_path.parent.mkdir(parents=True, exist_ok=True)
