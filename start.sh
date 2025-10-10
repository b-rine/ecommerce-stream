#!/bin/bash

echo "🚀 Starting E-commerce Kafka Analytics System (Kotlin/Spring Boot)"
echo "================================================================="

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker is not running. Please start Docker first."
    exit 1
fi

# Start Kafka infrastructure
echo "📦 Starting Kafka infrastructure..."
docker-compose up -d

# Wait for Kafka to be ready
echo "⏳ Waiting for Kafka to be ready..."
sleep 15

# Check if Java 17+ is available
if ! java -version 2>&1 | grep -q "version \"1[7-9]\|version \"[2-9]"; then
    echo "❌ Java 17+ is required. Please install Java 17 or higher."
    exit 1
fi

echo "☕ Java version check passed"

# Build and run the Spring Boot application
echo "🔨 Building Kotlin Spring Boot application..."
./gradlew clean build -x test

if [ $? -ne 0 ]; then
    echo "❌ Build failed. Please check the error messages above."
    exit 1
fi

echo "🌐 Starting Spring Boot application..."
echo "   Dashboard: http://localhost:8080"
echo "   Logs: http://localhost:8080/logs"
echo "   Database: http://localhost:8080/h2-console"
echo "   (Database URL: jdbc:h2:file:./data/ecommerce, Username: sa, Password: password)"
echo ""
echo "Press Ctrl+C to stop all services"

# Cleanup function
cleanup() {
    echo ""
    echo "🛑 Shutting down services..."
    docker-compose down
    echo "✅ All services stopped"
    exit 0
}

# Set up signal handlers
trap cleanup SIGINT SIGTERM

# Run the application
java -jar build/libs/kafka-ecommerce-stream-0.0.1-SNAPSHOT.jar