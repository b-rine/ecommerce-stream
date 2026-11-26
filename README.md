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

- Python 3.14
- Docker Desktop (or Docker Engine with the Compose v2 plugin, `docker compose`)
- Git
- `timeout`, used by `run_local.sh` (GNU coreutils; on macOS `brew install coreutils` and put its `gnubin` directory on `PATH`)

### Setup & Run

1. **Clone the repository**
    ```bash
    git clone https://github.com/b-rine/ecommerce-stream.git
    cd ecommerce-stream
    ```

2. **Install the Python dependencies**
    ```bash
    python3.14 -m venv .venv
    source .venv/bin/activate
    pip install -r requirements.txt
    ```

3. **Upgrading from an older checkout?** Kafka now runs as a single KRaft-mode node, and the old stack's containers and volumes are incompatible. This deletes them, including any Kafka data they hold:
    ```bash
    docker compose -f docker/docker-compose.yml down -v --remove-orphans
    ```

4. **Run setup executable** (starts Kafka with Docker Compose, then the backend locally)
    ```bash
    chmod +x scripts/run_local.sh
    ./scripts/run_local.sh
    ```

5. **Access the application**
    - Dashboard: http://localhost:8001
    - Use *START STREAM* to begin generating events.

## Smoke Test

CI starts the full stack in Docker (dashboard on http://localhost:8000) and checks that events flow end to end:

```bash
docker compose -f docker/docker-compose.yml up -d --build --wait
pip install "$(grep '^websockets==' requirements.txt)"
python scripts/smoke_test.py   # starts the stream, checks Kafka and the WebSocket, then stops it
docker compose -f docker/docker-compose.yml down -v
```

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.