package com.ecommerce.kafkaecommerce.service

import com.ecommerce.kafkaecommerce.model.OrderEvent
import com.ecommerce.kafkaecommerce.model.OrderItemDto
import com.ecommerce.kafkaecommerce.model.Product
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class OrderProducerService(
    private val kafkaTemplate: KafkaTemplate<String, Any>
) {
    
    private val logger = LoggerFactory.getLogger(OrderProducerService::class.java)
    
    fun sendOrder(orderId: String, customerName: String, customerEmail: String?, 
                 items: List<Pair<Product, Int>>, totalAmount: Double) {
        
        val orderItems = items.map { (product, quantity) ->
            OrderItemDto(
                productId = product.id!!,
                productName = product.name,
                category = product.category,
                price = product.price,
                quantity = quantity,
                total = product.price * quantity
            )
        }
        
        val orderEvent = OrderEvent(
            orderId = orderId,
            customerName = customerName,
            customerEmail = customerEmail,
            items = orderItems,
            totalAmount = totalAmount,
            timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            status = "PENDING"
        )
        
        try {
            kafkaTemplate.send("orders", orderId, orderEvent)
            logger.info("Order sent to Kafka: {} for customer: {}", orderId, customerName)
        } catch (e: Exception) {
            logger.error("Error sending order to Kafka: {}", e.message)
            throw e
        }
    }
}

