#!/bin/bash

# Script to run the application locally

set -e

echo "Starting Ecommerce Stream Application..."
echo ""

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "Error: Docker is not running. Please start Docker and try again."
    exit 1
fi

# Navigate to project root
cd "$(dirname "$0")/.."

# Start Docker Compose services
echo "Starting Docker Compose services (Kafka, Zookeeper)..."
docker-compose -f docker/docker-compose.yml up -d zookeeper kafka

# Wait for Kafka to be ready
echo "Waiting for Kafka to be ready..."
timeout 60 bash -c "until docker exec kafka kafka-broker-api-versions --bootstrap-server localhost:9092 > /dev/null 2>&1; do sleep 2; done" || {
    echo "Error: Kafka did not become ready in time"
    exit 1
}

echo "Kafka is ready!"
echo ""

# Set up Kafka topic (Kafka will auto-create if enabled, but we'll try to create it explicitly)
echo "Setting up Kafka topic..."
docker exec kafka kafka-topics --bootstrap-server localhost:9092 \
  --create --topic ecommerce-events --partitions 3 --replication-factor 1 --if-not-exists 2>/dev/null || {
    echo "Topic will be auto-created on first use (or already exists)"
}

echo ""

# Check if port 8000 is already in use
if lsof -Pi :8000 -sTCP:LISTEN -t >/dev/null 2>&1 || (command -v netstat >/dev/null && netstat -tuln 2>/dev/null | grep -q ':8000 '); then
    echo "❌ Error: Port 8000 is already in use"
    echo ""
    echo "To fix this:"
    echo "  1. Find what's using it: lsof -i :8000  (or: netstat -tuln | grep 8000)"
    echo "  2. Stop that process, or"
    echo "  3. Change port in .env: SERVER_PORT=8001"
    exit 1
fi

echo "Starting backend server..."
echo "Access the dashboard at: http://localhost:8001"
echo ""
echo "Press Ctrl+C to stop all services"
echo ""

# Run the backend (try python3 first, then python)
if command -v python3 &> /dev/null; then
    python3 -m uvicorn backend.app:app --host 0.0.0.0 --port 8001 --reload
elif command -v python &> /dev/null; then
    python -m uvicorn backend.app:app --host 0.0.0.0 --port 8001 --reload
else
    echo "Error: Python is not installed or not in PATH"
    exit 1
fi