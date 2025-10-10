package com.ecommerce.kafkaecommerce.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "orders")
data class Order(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    @Column(unique = true)
    val orderId: String,
    
    val customerName: String,
    val customerEmail: String?,
    val totalAmount: Double,
    val status: OrderStatus = OrderStatus.PENDING,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    
    @OneToMany(mappedBy = "order", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val items: MutableList<OrderItem> = mutableListOf()
)

@Entity
@Table(name = "order_items")
data class OrderItem(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    val order: Order? = null,
    
    val productId: Long,
    val productName: String,
    val category: String,
    val price: Double,
    val quantity: Int,
    val total: Double
)

enum class OrderStatus {
    PENDING, PROCESSING, COMPLETED, CANCELLED
}

// DTOs for Kafka messages
data class OrderEvent(
    val orderId: String,
    val customerName: String,
    val customerEmail: String?,
    val items: List<OrderItemDto>,
    val totalAmount: Double,
    val timestamp: String,
    val status: String
)

data class OrderItemDto(
    val productId: Long,
    val productName: String,
    val category: String,
    val price: Double,
    val quantity: Int,
    val total: Double
)

data class CreateOrderRequest(
    val customerName: String,
    val customerEmail: String?,
    val items: List<CreateOrderItemRequest>
)

data class CreateOrderItemRequest(
    val productId: Long,
    val quantity: Int
)

