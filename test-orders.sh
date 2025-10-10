#!/bin/bash

echo "🛒 Testing E-commerce Kafka System"
echo "=================================="

# Check if the application is running
if ! curl -s http://localhost:8080/api/stats > /dev/null; then
    echo "❌ Application is not running. Please start it with ./start.sh first."
    exit 1
fi

echo "✅ Application is running!"

# Sample order data
echo "📦 Sending test orders..."

# Order 1
curl -X POST http://localhost:8080/api/order \
  -H "Content-Type: application/json" \
  -d '{
    "customerName": "Alice Johnson",
    "customerEmail": "alice@example.com",
    "items": [
      {"productId": 1, "quantity": 1},
      {"productId": 3, "quantity": 2}
    ]
  }' && echo ""

sleep 2

# Order 2
curl -X POST http://localhost:8080/api/order \
  -H "Content-Type: application/json" \
  -d '{
    "customerName": "Bob Smith",
    "customerEmail": "bob@example.com",
    "items": [
      {"productId": 2, "quantity": 1},
      {"productId": 4, "quantity": 3}
    ]
  }' && echo ""

sleep 2

# Order 3
curl -X POST http://localhost:8080/api/order \
  -H "Content-Type: application/json" \
  -d '{
    "customerName": "Carol Davis",
    "customerEmail": "carol@example.com",
    "items": [
      {"productId": 5, "quantity": 1},
      {"productId": 6, "quantity": 1},
      {"productId": 7, "quantity": 2}
    ]
  }' && echo ""

echo ""
echo "✅ Test orders sent!"
echo ""
echo "📊 Current statistics:"
curl -s http://localhost:8080/api/stats | jq '.' 2>/dev/null || curl -s http://localhost:8080/api/stats

echo ""
echo "🌐 Check your dashboard at: http://localhost:8080"
echo "📋 View logs at: http://localhost:8080/logs"

