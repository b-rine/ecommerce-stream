package com.ecommerce.kafkaecommerce.service

import com.ecommerce.kafkaecommerce.model.*
import com.ecommerce.kafkaecommerce.repository.ProductRepository
import org.springframework.stereotype.Service
import java.util.*

@Service
class OrderService(
    private val productRepository: ProductRepository,
    private val orderProducerService: OrderProducerService
) {
    
    fun createOrder(request: CreateOrderRequest): String {
        val orderId = "ORD-${System.currentTimeMillis()}"
        
        // Validate products and calculate total
        val items = mutableListOf<Pair<Product, Int>>()
        var totalAmount = 0.0
        
        for (itemRequest in request.items) {
            val product = productRepository.findById(itemRequest.productId)
                .orElseThrow { IllegalArgumentException("Product not found: ${itemRequest.productId}") }
            
            items.add(product to itemRequest.quantity)
            totalAmount += product.price * itemRequest.quantity
        }
        
        // Send to Kafka
        orderProducerService.sendOrder(
            orderId = orderId,
            customerName = request.customerName,
            customerEmail = request.customerEmail,
            items = items,
            totalAmount = totalAmount
        )
        
        return orderId
    }
    
    fun getAllProducts(): List<Product> {
        return productRepository.findAll()
    }
}

