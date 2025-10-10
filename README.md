# E-commerce Kafka Analysis Project (Kotlin/Spring Boot)

A modern e-commerce analysis system built with Kotlin and Spring Boot, using Apache Kafka for real-time order processing and analytics.

## Features
- **Kotlin/Spring Boot** backend with modern architecture
- **Real-time Kafka** message processing
- **Web interface** for placing orders and viewing analytics
- **H2 Database** for persistent storage
- **Thymeleaf** templates with responsive design
- **Comprehensive logging** to files

## Quick Start

### Prerequisites
- Java 17 or higher
- Docker and Docker Compose
- Git

### Installation

1. **Clone and start the system:**
```bash
cd kafka-ecommerce-stream
./start.sh
```

2. **Access the application:**
- **Dashboard**: http://localhost:8080
- **Kafka Logs**: http://localhost:8080/logs  
- **Database Console**: http://localhost:8080/h2-console
  - URL: `jdbc:h2:file:./data/ecommerce`
  - Username: `sa`
  - Password: `password`

## Architecture
```
Web Interface (Thymeleaf) → Spring Boot Controller → Kafka Producer
                                    ↓
H2 Database ← Analytics Service ← Kafka Consumer ← Kafka Topic
```

## Tech Stack
- **Backend**: Kotlin + Spring Boot 3.2
- **Message Broker**: Apache Kafka
- **Database**: H2 (in-memory/file)
- **Frontend**: Thymeleaf + Bootstrap 5
- **Build Tool**: Gradle
- **Containerization**: Docker Compose

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

## Stopping the System
Press `Ctrl+C` in the terminal where you ran `./start.sh`

This will stop the Spring Boot application and Docker containers.