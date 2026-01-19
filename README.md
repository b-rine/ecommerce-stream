# Ecommerce Stream Dashboard

An ecommerce event streaming application built with Python, Apache Kafka, and FastAPI.

## Features

- **Real-time Event Streaming**: Kafka-based producer-consumer architecture for ecommerce events
- **Interactive Web Dashboard**: Modern, responsive dashboard with real-time updates via WebSocket
- **Event Types**: Simulates various ecommerce events:
  - Page views
  - Add to cart
  - Remove from cart
  - Checkout start
  - Purchase (with order details)
  - Product reviews
- **Real-time Visualizations**:
  - Revenue over time chart
  - Events by type distribution
  - Top categories by sales
  - Events per minute tracking
- **Statistics Tracking**:
  - Total events processed
  - Revenue tracking
  - Order counts
  - Unique users and products
  - Category-wise sales analytics

## Quick Start

### Prerequisites

- Python 3.12+
- Docker Desktop (or Docker + Docker Compose)
- Git

### Setup & Run

1. **Clone the repository**
    ```bash
    git clone https://github.com/b-rine/ecommerce-stream.git
    cd ecommerce-stream
    ```

2. **Run setup executable**
    ```bash
    chmod +x scripts/run_local.sh
    ./scripts/run_local.sh
    ```

3. **Access the application**
    - Dashboard: http://localhost:8001
    - Use *START STREAM* to begin generating events.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.