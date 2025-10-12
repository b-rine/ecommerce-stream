package com.ecommerce.kafkaecommerce.service;

import com.ecommerce.kafkaecommerce.model.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class OrderProducerService {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    private static final String ORDER_TOPIC = "order-events";
    private static final String ORDER_PROCESSING_TOPIC = "order-processing";

    public void sendOrderEvent(Order order) {
        kafkaTemplate.send(ORDER_TOPIC, order.getOrderId(), order);
        System.out.println("Sent order event to Kafka: " + order.getOrderId());
    }

    public void sendOrderForProcessing(Order order) {
        kafkaTemplate.send(ORDER_PROCESSING_TOPIC, order.getOrderId(), order);
        System.out.println("Sent order for processing: " + order.getOrderId());
    }
}
