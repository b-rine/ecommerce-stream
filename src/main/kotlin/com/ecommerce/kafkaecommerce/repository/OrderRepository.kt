package com.ecommerce.kafkaecommerce.repository

import com.ecommerce.kafkaecommerce.model.Order
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface OrderRepository : JpaRepository<Order, Long> {
    fun findByOrderId(orderId: String): Order?
    
    @Query("SELECT o FROM Order o ORDER BY o.createdAt DESC")
    fun findRecentOrders(): List<Order>
    
    @Query("SELECT COUNT(o) FROM Order o")
    fun getTotalOrderCount(): Long
    
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0.0) FROM Order o")
    fun getTotalRevenue(): Double
    
    @Query("SELECT COALESCE(AVG(o.totalAmount), 0.0) FROM Order o")
    fun getAverageOrderValue(): Double
}

