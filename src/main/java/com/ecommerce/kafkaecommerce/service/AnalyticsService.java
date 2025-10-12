package com.ecommerce.kafkaecommerce.service;

import com.ecommerce.kafkaecommerce.dto.OrderStatsDto;
import com.ecommerce.kafkaecommerce.dto.RecentOrderDto;
import com.ecommerce.kafkaecommerce.model.Order;
import com.ecommerce.kafkaecommerce.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    @Autowired
    private OrderRepository orderRepository;

    public OrderStatsDto getOrderStats() {
        LocalDateTime last24Hours = LocalDateTime.now().minusHours(24);
        
        Long totalOrders = orderRepository.countOrdersSince(last24Hours);
        BigDecimal totalRevenue = orderRepository.getTotalRevenueSince(last24Hours);
        BigDecimal averageOrderValue = orderRepository.getAverageOrderValueSince(last24Hours);
        
        List<RecentOrderDto> recentOrders = orderRepository.findRecentOrders()
            .stream()
            .limit(10)
            .map(order -> new RecentOrderDto(
                order.getOrderId(),
                order.getCustomerName(),
                order.getTotalAmount().doubleValue(),
                order.getCreatedAt().toString()
            ))
            .collect(Collectors.toList());
        
        return new OrderStatsDto(
            totalOrders,
            totalRevenue != null ? totalRevenue.doubleValue() : 0.0,
            averageOrderValue != null ? averageOrderValue.doubleValue() : 0.0,
            recentOrders
        );
    }

    public List<Order> getRecentOrders() {
        return orderRepository.findRecentOrders();
    }

    public List<Order> getOrdersByStatus(Order.OrderStatus status) {
        return orderRepository.findByStatus(status);
    }
}
