package com.ecommerce.kafkaecommerce.controller;

import com.ecommerce.kafkaecommerce.dto.CreateOrderRequest;
import com.ecommerce.kafkaecommerce.dto.CreateOrderResponse;
import com.ecommerce.kafkaecommerce.dto.OrderStatsDto;
import com.ecommerce.kafkaecommerce.model.Order;
import com.ecommerce.kafkaecommerce.model.Product;
import com.ecommerce.kafkaecommerce.repository.ProductRepository;
import com.ecommerce.kafkaecommerce.service.AnalyticsService;
import com.ecommerce.kafkaecommerce.service.LogService;
import com.ecommerce.kafkaecommerce.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WebController.class)
class WebControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AnalyticsService analyticsService;

    @MockBean
    private OrderService orderService;

    @MockBean
    private ProductRepository productRepository;

    @MockBean
    private LogService logService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    void dashboard_ShouldReturnIndexPage() throws Exception {
        // Given
        OrderStatsDto stats = new OrderStatsDto(0L, 0.0, 0.0, Arrays.asList());
        when(analyticsService.getOrderStats()).thenReturn(stats);
        when(productRepository.findAll()).thenReturn(Arrays.asList());

        // When & Then
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));
    }

    @Test
    void logs_ShouldReturnLogsPage() throws Exception {
        // Given
        when(logService.getRecentLogs(50)).thenReturn(Arrays.asList());

        // When & Then
        mockMvc.perform(get("/logs"))
                .andExpect(status().isOk())
                .andExpect(view().name("logs"));
    }

    @Test
    void orderDetails_ShouldReturnOrderDetailsPage() throws Exception {
        // Given
        Order order = new Order();
        order.setOrderId("ORD-001");
        order.setCustomerName("John Doe");
        order.setCustomerEmail("john@example.com");
        order.setTotalAmount(new BigDecimal("99.99"));
        order.setStatus(Order.OrderStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now());

        when(orderService.getOrderByOrderId("ORD-001")).thenReturn(Optional.of(order));

        // When & Then
        mockMvc.perform(get("/order/ORD-001"))
                .andExpect(status().isOk())
                .andExpect(view().name("order-details"))
                .andExpect(model().attribute("order", order));
    }

    @Test
    void orderDetails_ShouldReturnErrorWhenOrderNotFound() throws Exception {
        // Given
        when(orderService.getOrderByOrderId("ORD-999")).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/order/ORD-999"))
                .andExpect(status().isOk())
                .andExpect(view().name("error"))
                .andExpect(model().attribute("error", "Order not found: ORD-999"));
    }

    @Test
    void createOrder_ShouldReturnSuccessResponse() throws Exception {
        // Given
        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerName("John Doe");
        request.setCustomerEmail("john@example.com");
        CreateOrderRequest.OrderItemRequest item = new CreateOrderRequest.OrderItemRequest(1L, 2);
        request.setItems(Arrays.asList(item));

        Order createdOrder = new Order();
        createdOrder.setOrderId("ORD-001");
        createdOrder.setCustomerName("John Doe");
        createdOrder.setCustomerEmail("john@example.com");

        when(orderService.createOrderFromRequest(any(CreateOrderRequest.class))).thenReturn(createdOrder);

        // When & Then
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.orderId").value("ORD-001"))
                .andExpect(jsonPath("$.message").value("Order created successfully"));
    }

    @Test
    void createOrder_ShouldReturnErrorResponseOnException() throws Exception {
        // Given
        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerName("John Doe");
        request.setCustomerEmail("john@example.com");
        request.setItems(Arrays.asList());

        when(orderService.createOrderFromRequest(any(CreateOrderRequest.class)))
                .thenThrow(new RuntimeException("Test error"));

        // When & Then
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Test error"));
    }

    @Test
    void getStats_ShouldReturnOrderStats() throws Exception {
        // Given
        OrderStatsDto stats = new OrderStatsDto(5L, 1250.0, 250.0, Arrays.asList());
        when(analyticsService.getOrderStats()).thenReturn(stats);

        // When & Then
        mockMvc.perform(get("/api/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalOrders").value(5))
                .andExpect(jsonPath("$.totalRevenue").value(1250.0))
                .andExpect(jsonPath("$.averageOrderValue").value(250.0));
    }
}
