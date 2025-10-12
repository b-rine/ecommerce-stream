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

        // No sample orders - all orders will be user-generated
    }
}
