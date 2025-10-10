package com.ecommerce.kafkaecommerce.service

import com.ecommerce.kafkaecommerce.model.Order
import com.ecommerce.kafkaecommerce.model.OrderItem
import com.ecommerce.kafkaecommerce.model.OrderStatus
import com.ecommerce.kafkaecommerce.repository.OrderRepository
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class AnalyticsService(
    private val orderRepository: OrderRepository
) {
    
    private val logger = LoggerFactory.getLogger(AnalyticsService::class.java)
    
    @KafkaListener(topics = ["orders"], groupId = "analytics-group")
    @Transactional
    fun processOrder(orderEvent: com.ecommerce.kafkaecommerce.model.OrderEvent) {
        try {
            logger.info("Processing order from Kafka: {} for customer: {}", 
                       orderEvent.orderId, orderEvent.customerName)
            
            // Create and save order to database
            val order = Order(
                orderId = orderEvent.orderId,
                customerName = orderEvent.customerName,
                customerEmail = orderEvent.customerEmail,
                totalAmount = orderEvent.totalAmount,
                status = OrderStatus.PROCESSING,
                createdAt = LocalDateTime.parse(orderEvent.timestamp, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            )
            
            val savedOrder = orderRepository.save(order)
            
            // Create and save order items
            val orderItems = orderEvent.items.map { itemDto ->
                OrderItem(
                    order = savedOrder,
                    productId = itemDto.productId,
                    productName = itemDto.productName,
                    category = itemDto.category,
                    price = itemDto.price,
                    quantity = itemDto.quantity,
                    total = itemDto.total
                )
            }
            
            savedOrder.items.addAll(orderItems)
            orderRepository.save(savedOrder)
            
            logger.info("Order processed successfully: {} - Total: ${}", 
                       orderEvent.orderId, orderEvent.totalAmount)
            
        } catch (e: Exception) {
            logger.error("Error processing order: {}", e.message, e)
        }
    }
}

