package com.ecommerce.kafkaecommerce.repository;

import com.ecommerce.kafkaecommerce.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    Optional<Order> findByOrderId(String orderId);
    
    List<Order> findByStatus(Order.OrderStatus status);
    
    List<Order> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    
    @Query("SELECT COUNT(o) FROM Order o WHERE o.createdAt >= :since")
    Long countOrdersSince(@Param("since") LocalDateTime since);
    
    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.createdAt >= :since")
    BigDecimal getTotalRevenueSince(@Param("since") LocalDateTime since);
    
    @Query("SELECT AVG(o.totalAmount) FROM Order o WHERE o.createdAt >= :since")
    BigDecimal getAverageOrderValueSince(@Param("since") LocalDateTime since);
    
    @Query("SELECT o FROM Order o ORDER BY o.createdAt DESC")
    List<Order> findRecentOrders();
}
