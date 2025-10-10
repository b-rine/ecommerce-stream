package com.ecommerce.kafkaecommerce.model

import jakarta.persistence.*

@Entity
@Table(name = "products")
data class Product(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    val name: String,
    val category: String,
    val price: Double,
    val description: String? = null
)

