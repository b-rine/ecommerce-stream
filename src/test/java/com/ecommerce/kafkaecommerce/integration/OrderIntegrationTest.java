package com.ecommerce.kafkaecommerce.integration;

import com.ecommerce.kafkaecommerce.dto.CreateOrderRequest;
import com.ecommerce.kafkaecommerce.model.Order;
import com.ecommerce.kafkaecommerce.model.Product;
import com.ecommerce.kafkaecommerce.repository.OrderRepository;
import com.ecommerce.kafkaecommerce.repository.ProductRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
@TestPropertySource(properties = {
    "spring.kafka.enabled=false",
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration"
})
class OrderIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        orderRepository.deleteAll();
        productRepository.deleteAll();

        // Create test products
        Product product1 = new Product("Laptop", "Electronics", new BigDecimal("999.99"), 10, "High-performance laptop");
        Product product2 = new Product("Mouse", "Electronics", new BigDecimal("29.99"), 50, "Wireless mouse");
        productRepository.saveAll(Arrays.asList(product1, product2));
    }

    @Test
    void createOrder_ShouldCreateOrderWithCorrectIdFormat() throws Exception {
        // Given
        CreateOrderRequest.OrderItemRequest item1 = new CreateOrderRequest.OrderItemRequest(1L, 1);
        CreateOrderRequest.OrderItemRequest item2 = new CreateOrderRequest.OrderItemRequest(2L, 2);
        CreateOrderRequest request = new CreateOrderRequest("John Doe", "john@example.com", Arrays.asList(item1, item2));

        // When
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.orderId").value("ORD-001"));

        // Then
        assertEquals(1, orderRepository.count());
        Order savedOrder = orderRepository.findAll().get(0);
        assertEquals("ORD-001", savedOrder.getOrderId());
        assertEquals("John Doe", savedOrder.getCustomerName());
        assertEquals(new BigDecimal("1059.97"), savedOrder.getTotalAmount()); // 999.99 + (29.99 * 2)
        assertEquals(2, savedOrder.getItems().size());
    }

    @Test
    void createMultipleOrders_ShouldGenerateSequentialIds() throws Exception {
        // Given
        CreateOrderRequest request1 = new CreateOrderRequest("Customer 1", "customer1@example.com", 
                Arrays.asList(new CreateOrderRequest.OrderItemRequest(1L, 1)));
        CreateOrderRequest request2 = new CreateOrderRequest("Customer 2", "customer2@example.com", 
                Arrays.asList(new CreateOrderRequest.OrderItemRequest(2L, 1)));

        // When
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value("ORD-001"));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value("ORD-002"));

        // Then
        assertEquals(2, orderRepository.count());
        assertTrue(orderRepository.findByOrderId("ORD-001").isPresent());
        assertTrue(orderRepository.findByOrderId("ORD-002").isPresent());
    }

    @Test
    void getOrderDetails_ShouldReturnOrderDetailsPage() throws Exception {
        // Given
        CreateOrderRequest request = new CreateOrderRequest("Test Customer", "test@example.com", 
                Arrays.asList(new CreateOrderRequest.OrderItemRequest(1L, 1)));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // When & Then
        mockMvc.perform(get("/order/ORD-001"))
                .andExpect(status().isOk())
                .andExpect(view().name("order-details"))
                .andExpect(model().attributeExists("order"));
    }

    @Test
    void createOrderWithInvalidProduct_ShouldReturnError() throws Exception {
        // Given
        CreateOrderRequest request = new CreateOrderRequest("Test Customer", "test@example.com", 
                Arrays.asList(new CreateOrderRequest.OrderItemRequest(999L, 1)));

        // When & Then
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Product not found with ID: 999"));
    }

    @Test
    void dashboard_ShouldDisplayProducts() throws Exception {
        // When & Then
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeExists("products"));
    }
}
