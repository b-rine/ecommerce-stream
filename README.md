# E-commerce Kafka Analytics (Java/Spring Boot)

A modern e-commerce analytics system built with Java 21 and Spring Boot 3.4, using Apache Kafka for real-time order processing and analytics.

[![Deploy on Railway](https://railway.app/button.svg)](https://railway.app/new/template?template=https://github.com/b-rine/ecommerce-stream)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Kafka](https://img.shields.io/badge/Apache%20Kafka-Latest-blue.svg)](https://kafka.apache.org/)

## Features
- ✅ **Standardized Order IDs** (ORD-001, ORD-002, etc.)
- ✅ **Real-time Kafka** message processing
- ✅ **Interactive Order Details** with comprehensive views
- ✅ **Live Analytics Dashboard** with auto-refresh
- ✅ **Real-time Kafka Logs** from actual log files
- ✅ **Comprehensive Test Suite** (Unit + Integration tests)
- ✅ **GitHub Actions CI/CD** with automated deployment
- ✅ **Modern Stack**: Java 21, Spring Boot 3.4, Gradle 9.1

## Quick Start

### Prerequisites
- **Java 21** or higher (OpenJDK recommended)
- **Docker and Docker Compose**
- **Git**

> **Note**: This project uses Java 21 and Gradle 9.1 for optimal performance and modern language features.

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
- **Backend**: Java 21 + Spring Boot 3.4
- **Message Broker**: Apache Kafka
- **Database**: H2 (file-based)
- **Frontend**: Thymeleaf + Bootstrap 5
- **Build Tool**: Gradle 9.1
- **Testing**: JUnit 5 + MockMvc
- **CI/CD**: GitHub Actions
- **Containerization**: Docker Compose

## How to Use

### 1. Place Orders
- Fill out customer information
- Select products and quantities  
- Click "Place Order" to send to Kafka
- Orders get standardized IDs (ORD-001, ORD-002, etc.)
- Watch real-time processing in logs

### 2. View Order Details
- Click any order ID in the recent orders list
- View comprehensive order details page
- See order timeline, items, and customer info
- Print order details

### 3. View Analytics
- Dashboard shows live statistics
- Total orders, revenue, average order value
- Recent orders list updates automatically
- Kafka status indicator (solid green circle)

### 4. Monitor System
- **Logs page**: Real-time Kafka message flow from actual log files
- **Database console**: View stored orders and products
- **Application logs**: Saved to `kafka-ecommerce.log`

## Testing

The project includes comprehensive tests:
- **Unit Tests**: Service layer testing with mocked dependencies
- **Integration Tests**: Full application context testing
- **Controller Tests**: API endpoint validation

Run tests with:
```bash
gradle test
```

## Deployment Options

### 🚀 Railway (Recommended - One Click Deploy)
**Option 1: Deploy Button**
Click the "Deploy on Railway" button above for instant deployment!

**Option 2: Manual Setup**
1. Sign up at [Railway](https://railway.app) with GitHub
2. Click "New Project" → "Deploy from GitHub repo"
3. Select your `ecommerce-stream` repository
4. Railway auto-detects Java/Spring Boot and deploys
5. Your app: `https://your-app.railway.app`

**Environment Variables (Optional):**
```
SPRING_PROFILES_ACTIVE=production
DATABASE_PASSWORD=your-secure-password
```

### 🐳 Docker Deployment
```bash
# Build the Docker image
docker build -t ecommerce-kafka .

# Run with Docker Compose (includes Kafka)
docker-compose up -d
```

### ☁️ Other Cloud Providers
- **Heroku**: Add Procfile and deploy
- **Fly.io**: Use provided fly.toml
- **AWS/GCP/Azure**: Use container services

## CI/CD Pipeline

GitHub Actions automatically:
- ✅ Runs tests on every push/PR
- ✅ Builds the application
- ✅ Deploys to Railway (on main branch)
- ✅ Validates Java 21 compatibility

## Stopping the System
Press `Ctrl+C` in the terminal where you ran `./start.sh`

This will stop the Spring Boot application and Docker containers.