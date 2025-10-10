# Kotlin E-commerce Kafka Setup Guide

## Prerequisites
- **Java 17+** (OpenJDK recommended)
- **Docker & Docker Compose**
- **Git**

## Quick Setup

1. **Start the entire system:**
   ```bash
   ./start.sh
   ```
   
   This single command will:
   - Start Kafka and Zookeeper via Docker
   - Build the Kotlin Spring Boot application
   - Start the web application with embedded server

2. **Access the application:**
   - **Main Dashboard**: http://localhost:8080
   - **Kafka Logs**: http://localhost:8080/logs
   - **Database Console**: http://localhost:8080/h2-console

## Manual Setup (Alternative)

If you prefer to run components separately:

1. **Start Kafka:**
   ```bash
   docker-compose up -d
   ```

2. **Build the application:**
   ```bash
   ./gradlew clean build
   ```

3. **Run the application:**
   ```bash
   java -jar build/libs/kafka-ecommerce-stream-0.0.1-SNAPSHOT.jar
   ```

## How to Use

### 1. Place Orders
- Fill out customer information
- Select products and quantities  
- Click "Place Order" to send to Kafka
- Watch real-time processing in logs

### 2. View Analytics
- Dashboard shows live statistics
- Total orders, revenue, average order value
- Recent orders list updates automatically
- Kafka status indicator

### 3. Monitor System
- **Logs page**: Real-time Kafka message flow
- **Database console**: View stored orders and products
- **Application logs**: Saved to `kafka-ecommerce.log`

## Project Structure

```
src/main/kotlin/com/ecommerce/kafkaecommerce/
├── KafkaEcommerceApplication.kt      # Main Spring Boot app
├── config/
│   ├── KafkaConfig.kt               # Kafka configuration
│   └── DataInitializer.kt           # Sample data setup
├── controller/
│   └── WebController.kt             # REST endpoints & web routes
├── service/
│   ├── OrderService.kt              # Order business logic
│   ├── OrderProducerService.kt      # Kafka producer
│   └── AnalyticsService.kt          # Kafka consumer
├── repository/
│   ├── OrderRepository.kt           # Order data access
│   └── ProductRepository.kt         # Product data access
├── model/
│   ├── Order.kt                     # Order entities
│   └── Product.kt                   # Product entities
└── dto/
    └── AnalyticsDto.kt              # Data transfer objects
```

## Key Features

### Kafka Integration
- **Producer**: Sends orders to `orders` topic
- **Consumer**: Processes orders and saves to database
- **Real-time**: Messages processed as they arrive

### Data Persistence
- **H2 Database**: Lightweight, file-based SQL database
- **JPA Entities**: Order and Product models
- **Automatic Schema**: Created on startup

### Logging
- **File Logging**: All logs saved to `kafka-ecommerce.log`
- **Structured Logs**: Timestamp, level, logger, message
- **Kafka Events**: Order processing tracked

## Stopping the System
Press `Ctrl+C` in the terminal where you ran `./start.sh`

This will stop the Spring Boot application and Docker containers.

## Troubleshooting

### Java Version Issues
```bash
# Check Java version
java -version

# Should show Java 17 or higher
```

### Docker Issues
```bash
# Check Docker is running
docker info

# Restart Docker if needed
sudo systemctl restart docker
```

### Build Issues
```bash
# Clean and rebuild
./gradlew clean build --info

# Check for dependency issues
./gradlew dependencies
```