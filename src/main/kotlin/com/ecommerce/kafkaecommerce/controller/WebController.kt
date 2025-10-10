package com.ecommerce.kafkaecommerce.controller

import com.ecommerce.kafkaecommerce.dto.*
import com.ecommerce.kafkaecommerce.model.CreateOrderRequest
import com.ecommerce.kafkaecommerce.service.OrderService
import com.ecommerce.kafkaecommerce.repository.OrderRepository
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*
import org.springframework.http.ResponseEntity
import java.time.format.DateTimeFormatter

@Controller
class WebController(
    private val orderService: OrderService,
    private val orderRepository: OrderRepository
) {
    
    @GetMapping("/")
    fun index(model: Model): String {
        model.addAttribute("products", orderService.getAllProducts())
        return "index"
    }
    
    @PostMapping("/api/order")
    @ResponseBody
    fun createOrder(@RequestBody request: CreateOrderRequest): ResponseEntity<Map<String, Any>> {
        return try {
            val orderId = orderService.createOrder(request)
            ResponseEntity.ok(mapOf(
                "success" to true,
                "orderId" to orderId,
                "message" to "Order placed successfully!"
            ))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(mapOf(
                "success" to false,
                "error" to e.message
            ))
        }
    }
    
    @GetMapping("/api/stats")
    @ResponseBody
    fun getStats(): ResponseEntity<OrderStatsDto> {
        val totalOrders = orderRepository.getTotalOrderCount()
        val totalRevenue = orderRepository.getTotalRevenue()
        val averageOrderValue = orderRepository.getAverageOrderValue()
        
        val recentOrders = orderRepository.findRecentOrders().take(10).map { order ->
            RecentOrderDto(
                orderId = order.orderId,
                customerName = order.customerName,
                totalAmount = order.totalAmount,
                createdAt = order.createdAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            )
        }
        
        val stats = OrderStatsDto(
            totalOrders = totalOrders,
            totalRevenue = totalRevenue,
            averageOrderValue = averageOrderValue,
            recentOrders = recentOrders
        )
        
        return ResponseEntity.ok(stats)
    }
    
    @GetMapping("/logs")
    fun logs(): String {
        return "logs"
    }
}

