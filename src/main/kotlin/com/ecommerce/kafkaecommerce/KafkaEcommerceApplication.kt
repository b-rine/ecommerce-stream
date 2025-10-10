package com.ecommerce.kafkaecommerce

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.kafka.annotation.EnableKafka

@SpringBootApplication
@EnableKafka
class KafkaEcommerceApplication

fun main(args: Array<String>) {
    runApplication<KafkaEcommerceApplication>(*args)
}

