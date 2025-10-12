package com.ecommerce.kafkaecommerce.config;

import com.ecommerce.kafkaecommerce.model.Order;
import com.ecommerce.kafkaecommerce.model.Product;
import com.ecommerce.kafkaecommerce.repository.OrderRepository;
import com.ecommerce.kafkaecommerce.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Override
    public void run(String... args) throws Exception {
        // Initialize sample products
        if (productRepository.count() == 0) {
            List<Product> products = Arrays.asList(
                new Product("Laptop", "Electronics", new BigDecimal("999.99"), 50, "High-performance laptop"),
                new Product("Smartphone", "Electronics", new BigDecimal("699.99"), 100, "Latest smartphone"),
                new Product("Headphones", "Electronics", new BigDecimal("199.99"), 200, "Wireless headphones"),
                new Product("T-Shirt", "Clothing", new BigDecimal("29.99"), 500, "Cotton t-shirt"),
                new Product("Jeans", "Clothing", new BigDecimal("79.99"), 300, "Denim jeans"),
                new Product("Sneakers", "Footwear", new BigDecimal("129.99"), 150, "Comfortable sneakers"),
                new Product("Backpack", "Accessories", new BigDecimal("59.99"), 100, "Travel backpack"),
                new Product("Watch", "Accessories", new BigDecimal("299.99"), 75, "Digital watch")
            );
            productRepository.saveAll(products);
        }

        // Initialize sample orders
        if (orderRepository.count() == 0) {
            List<Order> orders = Arrays.asList(
                new Order("ORD-001", "John Doe", "john@example.com", 
                         new BigDecimal("999.99"), Order.OrderStatus.CONFIRMED,
                         Arrays.asList(new Order.OrderItem("Laptop", 1, new BigDecimal("999.99")))),
                new Order("ORD-002", "Jane Smith", "jane@example.com", 
                         new BigDecimal("129.98"), Order.OrderStatus.SHIPPED,
                         Arrays.asList(
                             new Order.OrderItem("T-Shirt", 2, new BigDecimal("29.99")),
                             new Order.OrderItem("Sneakers", 1, new BigDecimal("129.99"))
                         )),
                new Order("ORD-003", "Bob Johnson", "bob@example.com", 
                         new BigDecimal("699.99"), Order.OrderStatus.PENDING,
                         Arrays.asList(new Order.OrderItem("Smartphone", 1, new BigDecimal("699.99"))))
            );
            orderRepository.saveAll(orders);
        }
    }
}
