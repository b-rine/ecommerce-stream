package com.ecommerce.kafkaecommerce.service;

import com.ecommerce.kafkaecommerce.dto.CreateOrderRequest;
import com.ecommerce.kafkaecommerce.model.Order;
import com.ecommerce.kafkaecommerce.model.Product;
import com.ecommerce.kafkaecommerce.repository.OrderRepository;
import com.ecommerce.kafkaecommerce.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderProducerService orderProducerService;

    @InjectMocks
    private OrderService orderService;

    private Product testProduct;
    private CreateOrderRequest testRequest;

    @BeforeEach
    void setUp() {
        testProduct = new Product("Test Product", "Electronics", new BigDecimal("99.99"), 10, "Test description");
        testProduct.setId(1L);

        CreateOrderRequest.OrderItemRequest itemRequest = new CreateOrderRequest.OrderItemRequest(1L, 2);
        testRequest = new CreateOrderRequest("John Doe", "john@example.com", Arrays.asList(itemRequest));
    }

    @Test
    void createOrderFromRequest_ShouldCreateOrderWithCorrectFormat() {
        // Given
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(orderRepository.count()).thenReturn(5L);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(1L);
            return order;
        });

        // When
        Order result = orderService.createOrderFromRequest(testRequest);

        // Then
        assertNotNull(result);
        assertEquals("ORD-006", result.getOrderId()); // Should be 6th order (count + 1)
        assertEquals("John Doe", result.getCustomerName());
        assertEquals("john@example.com", result.getCustomerEmail());
        assertEquals(new BigDecimal("199.98"), result.getTotalAmount()); // 99.99 * 2
        assertEquals(Order.OrderStatus.PENDING, result.getStatus());
        assertEquals(1, result.getItems().size());
        assertEquals("Test Product", result.getItems().get(0).getProductName());
        assertEquals(2, result.getItems().get(0).getQuantity());

        verify(orderRepository).save(any(Order.class));
        verify(orderProducerService).sendOrderEvent(any(Order.class));
    }

    @Test
    void createOrderFromRequest_ShouldThrowExceptionWhenProductNotFound() {
        // Given
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            orderService.createOrderFromRequest(testRequest);
        });

        assertEquals("Product not found with ID: 1", exception.getMessage());
        verify(orderRepository, never()).save(any(Order.class));
        verify(orderProducerService, never()).sendOrderEvent(any(Order.class));
    }

    @Test
    void createOrderFromRequest_ShouldCalculateCorrectTotalForMultipleItems() {
        // Given
        Product product2 = new Product("Product 2", "Electronics", new BigDecimal("49.99"), 5, "Description");
        product2.setId(2L);

        CreateOrderRequest.OrderItemRequest item1 = new CreateOrderRequest.OrderItemRequest(1L, 2);
        CreateOrderRequest.OrderItemRequest item2 = new CreateOrderRequest.OrderItemRequest(2L, 1);
        CreateOrderRequest request = new CreateOrderRequest("Jane Doe", "jane@example.com", Arrays.asList(item1, item2));

        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(productRepository.findById(2L)).thenReturn(Optional.of(product2));
        when(orderRepository.count()).thenReturn(0L);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(1L);
            return order;
        });

        // When
        Order result = orderService.createOrderFromRequest(request);

        // Then
        assertEquals(new BigDecimal("249.97"), result.getTotalAmount()); // (99.99 * 2) + (49.99 * 1)
        assertEquals(2, result.getItems().size());
    }

    @Test
    void generateOrderId_ShouldFormatCorrectly() {
        // Given
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(orderRepository.count()).thenReturn(0L);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(1L);
            return order;
        });

        // When
        Order result = orderService.createOrderFromRequest(testRequest);

        // Then
        assertEquals("ORD-001", result.getOrderId());
    }

    @Test
    void generateOrderId_ShouldFormatCorrectlyForHighNumbers() {
        // Given
        when(orderRepository.count()).thenReturn(999L);
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(1L);
            return order;
        });

        // When
        Order result = orderService.createOrderFromRequest(testRequest);

        // Then
        assertEquals("ORD-1000", result.getOrderId());
    }
}
