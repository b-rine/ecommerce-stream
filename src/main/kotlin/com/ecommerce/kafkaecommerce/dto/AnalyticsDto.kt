package com.ecommerce.kafkaecommerce.dto

data class OrderStatsDto(
    val totalOrders: Long,
    val totalRevenue: Double,
    val averageOrderValue: Double,
    val recentOrders: List<RecentOrderDto>
)

data class RecentOrderDto(
    val orderId: String,
    val customerName: String,
    val totalAmount: Double,
    val createdAt: String
)

data class ProductStatsDto(
    val productName: String,
    val totalQuantity: Int
)

data class CategoryStatsDto(
    val category: String,
    val totalQuantity: Int
)

data class HourlyStatsDto(
    val hour: Int,
    val orderCount: Long
)

