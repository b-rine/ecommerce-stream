package com.ecommerce.kafkaecommerce.dto;

import java.util.List;

public class OrderStatsDto {
    private Long totalOrders;
    private Double totalRevenue;
    private Double averageOrderValue;
    private List<RecentOrderDto> recentOrders;
    
    public OrderStatsDto() {}
    
    public OrderStatsDto(Long totalOrders, Double totalRevenue, Double averageOrderValue, List<RecentOrderDto> recentOrders) {
        this.totalOrders = totalOrders;
        this.totalRevenue = totalRevenue;
        this.averageOrderValue = averageOrderValue;
        this.recentOrders = recentOrders;
    }
    
    // Getters and Setters
    public Long getTotalOrders() {
        return totalOrders;
    }
    
    public void setTotalOrders(Long totalOrders) {
        this.totalOrders = totalOrders;
    }
    
    public Double getTotalRevenue() {
        return totalRevenue;
    }
    
    public void setTotalRevenue(Double totalRevenue) {
        this.totalRevenue = totalRevenue;
    }
    
    public Double getAverageOrderValue() {
        return averageOrderValue;
    }
    
    public void setAverageOrderValue(Double averageOrderValue) {
        this.averageOrderValue = averageOrderValue;
    }
    
    public List<RecentOrderDto> getRecentOrders() {
        return recentOrders;
    }
    
    public void setRecentOrders(List<RecentOrderDto> recentOrders) {
        this.recentOrders = recentOrders;
    }
}