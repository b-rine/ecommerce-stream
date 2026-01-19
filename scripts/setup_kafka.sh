#!/bin/bash

# Script to set up Kafka topic for ecommerce events

KAFKA_BOOTSTRAP_SERVERS=${KAFKA_BOOTSTRAP_SERVERS:-localhost:9092}
TOPIC_NAME=${KAFKA_TOPIC:-ecommerce-events}
PARTITIONS=${KAFKA_PARTITIONS:-3}
REPLICATION_FACTOR=${KAFKA_REPLICATION_FACTOR:-1}

echo "Setting up Kafka topic: $TOPIC_NAME"
echo "Bootstrap servers: $KAFKA_BOOTSTRAP_SERVERS"
echo "Partitions: $PARTITIONS"
echo "Replication factor: $REPLICATION_FACTOR"

# Check if Kafka is available
echo "Checking Kafka availability..."
timeout 10 bash -c "until kafka-broker-api-versions --bootstrap-server $KAFKA_BOOTSTRAP_SERVERS; do sleep 1; done" || {
    echo "Error: Cannot connect to Kafka at $KAFKA_BOOTSTRAP_SERVERS"
    echo "Make sure Kafka is running and accessible"
    exit 1
}

# Check if topic already exists
if kafka-topics.sh --bootstrap-server $KAFKA_BOOTSTRAP_SERVERS --list | grep -q "^${TOPIC_NAME}$"; then
    echo "Topic $TOPIC_NAME already exists"
    echo "Topic details:"
    kafka-topics.sh --bootstrap-server $KAFKA_BOOTSTRAP_SERVERS --describe --topic $TOPIC_NAME
else
    echo "Creating topic $TOPIC_NAME..."
    kafka-topics.sh --bootstrap-server $KAFKA_BOOTSTRAP_SERVERS \
        --create \
        --topic $TOPIC_NAME \
        --partitions $PARTITIONS \
        --replication-factor $REPLICATION_FACTOR
    
    if [ $? -eq 0 ]; then
        echo "Topic $TOPIC_NAME created successfully"
        echo "Topic details:"
        kafka-topics.sh --bootstrap-server $KAFKA_BOOTSTRAP_SERVERS --describe --topic $TOPIC_NAME
    else
        echo "Error: Failed to create topic"
        exit 1
    fi
fi

echo "Kafka setup complete!"
