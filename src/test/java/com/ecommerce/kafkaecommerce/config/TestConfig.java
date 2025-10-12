package com.ecommerce.kafkaecommerce.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import com.ecommerce.kafkaecommerce.service.OrderProducerService;

@TestConfiguration
@Profile("test")
public class TestConfig {

    @Bean
    @Primary
    public OrderProducerService mockOrderProducerService() {
        return new OrderProducerService() {
            @Override
            public void sendOrderEvent(com.ecommerce.kafkaecommerce.model.Order order) {
                // Mock implementation - do nothing in tests
                System.out.println("Mock: Order sent to Kafka: " + order.getOrderId());
            }
        };
    }
}
