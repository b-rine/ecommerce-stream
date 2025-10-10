package com.ecommerce.kafkaecommerce.config

import com.ecommerce.kafkaecommerce.model.Product
import com.ecommerce.kafkaecommerce.repository.ProductRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component

@Component
class DataInitializer(
    private val productRepository: ProductRepository
) : CommandLineRunner {
    
    private val logger = LoggerFactory.getLogger(DataInitializer::class.java)
    
    override fun run(vararg args: String?) {
        if (productRepository.count() == 0L) {
            logger.info("Initializing sample products...")
            
            val products = listOf(
                Product(name = "Laptop", category = "Electronics", price = 999.99, description = "High-performance laptop"),
                Product(name = "Smartphone", category = "Electronics", price = 699.99, description = "Latest smartphone"),
                Product(name = "Headphones", category = "Electronics", price = 199.99, description = "Wireless headphones"),
                Product(name = "T-Shirt", category = "Clothing", price = 29.99, description = "Cotton t-shirt"),
                Product(name = "Jeans", category = "Clothing", price = 79.99, description = "Denim jeans"),
                Product(name = "Running Shoes", category = "Sports", price = 129.99, description = "Athletic running shoes"),
                Product(name = "Coffee Mug", category = "Home", price = 15.99, description = "Ceramic coffee mug"),
                Product(name = "Book", category = "Books", price = 19.99, description = "Programming book")
            )
            
            productRepository.saveAll(products)
            logger.info("Sample products initialized successfully")
        }
    }
}

