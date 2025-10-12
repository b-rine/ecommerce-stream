package com.ecommerce.kafkaecommerce.service;

import com.ecommerce.kafkaecommerce.dto.CreateOrderRequest;
import com.ecommerce.kafkaecommerce.model.Order;
import com.ecommerce.kafkaecommerce.model.Product;
import com.ecommerce.kafkaecommerce.repository.OrderRepository;
import com.ecommerce.kafkaecommerce.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderProducerService orderProducerService;

    public Order createOrder(Order order) {
        if (order.getOrderId() == null) {
            order.setOrderId(UUID.randomUUID().toString());
        }
        order.setCreatedAt(LocalDateTime.now());
        order.setStatus(Order.OrderStatus.PENDING);
        
        Order savedOrder = orderRepository.save(order);
        
        // Send to Kafka for processing
        orderProducerService.sendOrderEvent(savedOrder);
        
        return savedOrder;
    }

    public Order createOrderFromRequest(CreateOrderRequest request) {
        // Calculate total amount and create order items
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<Order.OrderItem> orderItems = new ArrayList<>();
        
        for (CreateOrderRequest.OrderItemRequest itemRequest : request.getItems()) {
            Optional<Product> productOpt = productRepository.findById(itemRequest.getProductId());
            if (!productOpt.isPresent()) {
                throw new RuntimeException("Product not found with ID: " + itemRequest.getProductId());
            }
            
            Product product = productOpt.get();
            BigDecimal itemTotal = product.getPrice().multiply(BigDecimal.valueOf(itemRequest.getQuantity()));
            totalAmount = totalAmount.add(itemTotal);
            
            Order.OrderItem orderItem = new Order.OrderItem(
                product.getName(),
                itemRequest.getQuantity(),
                product.getPrice()
            );
            orderItems.add(orderItem);
        }
        
        // Create order
        Order order = new Order();
        order.setOrderId(UUID.randomUUID().toString());
        order.setCustomerName(request.getCustomerName());
        order.setCustomerEmail(request.getCustomerEmail());
        order.setTotalAmount(totalAmount);
        order.setStatus(Order.OrderStatus.PENDING);
        order.setItems(orderItems);
        order.setCreatedAt(LocalDateTime.now());
        
        Order savedOrder = orderRepository.save(order);
        
        // Send to Kafka for processing
        orderProducerService.sendOrderEvent(savedOrder);
        
        return savedOrder;
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public Optional<Order> getOrderById(Long id) {
        return orderRepository.findById(id);
    }

    public Optional<Order> getOrderByOrderId(String orderId) {
        return orderRepository.findByOrderId(orderId);
    }

    public Order updateOrderStatus(Long id, Order.OrderStatus status) {
        Optional<Order> orderOpt = orderRepository.findById(id);
        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();
            order.setStatus(status);
            order.setUpdatedAt(LocalDateTime.now());
            return orderRepository.save(order);
        }
        return null;
    }

    public List<Order> getOrdersByStatus(Order.OrderStatus status) {
        return orderRepository.findByStatus(status);
    }

    public List<Order> getOrdersByDateRange(LocalDateTime start, LocalDateTime end) {
        return orderRepository.findByCreatedAtBetween(start, end);
    }

    @KafkaListener(topics = "order-processing", groupId = "ecommerce-group")
    public void processOrder(Order order) {
        System.out.println("Processing order: " + order.getOrderId());
        
        // Simulate order processing
        try {
            Thread.sleep(1000); // Simulate processing time
            
            // Update order status
            order.setStatus(Order.OrderStatus.CONFIRMED);
            orderRepository.save(order);
            
            System.out.println("Order confirmed: " + order.getOrderId());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Order processing interrupted: " + order.getOrderId());
        }
    }
}
