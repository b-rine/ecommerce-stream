package com.ecommerce.kafkaecommerce.controller;

import com.ecommerce.kafkaecommerce.dto.CreateOrderRequest;
import com.ecommerce.kafkaecommerce.dto.CreateOrderResponse;
import com.ecommerce.kafkaecommerce.dto.OrderStatsDto;
import com.ecommerce.kafkaecommerce.model.Order;
import com.ecommerce.kafkaecommerce.model.Product;
import com.ecommerce.kafkaecommerce.repository.ProductRepository;
import com.ecommerce.kafkaecommerce.service.AnalyticsService;
import com.ecommerce.kafkaecommerce.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Controller
public class WebController {

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductRepository productRepository;

    @GetMapping("/")
    public String dashboard(Model model) {
        OrderStatsDto stats = analyticsService.getOrderStats();
        List<Product> products = productRepository.findAll();
        model.addAttribute("stats", stats);
        model.addAttribute("products", products);
        return "index";
    }

    @GetMapping("/logs")
    public String logs(Model model) {
        List<Order> recentOrders = analyticsService.getRecentOrders();
        model.addAttribute("orders", recentOrders);
        return "logs";
    }

    @GetMapping("/api/stats")
    @ResponseBody
    public OrderStatsDto getStats() {
        return analyticsService.getOrderStats();
    }

    @GetMapping("/api/orders")
    @ResponseBody
    public List<Order> getOrders() {
        return orderService.getAllOrders();
    }

    @PostMapping("/api/orders")
    @ResponseBody
    public CreateOrderResponse createOrder(@RequestBody CreateOrderRequest request) {
        try {
            System.out.println("Received order request: " + request.getCustomerName() + " - " + request.getItems().size() + " items");
            Order order = orderService.createOrderFromRequest(request);
            System.out.println("Order created successfully: " + order.getOrderId());
            return new CreateOrderResponse(true, order.getOrderId(), "Order created successfully");
        } catch (Exception e) {
            System.err.println("Error creating order: " + e.getMessage());
            e.printStackTrace();
            return new CreateOrderResponse(false, e.getMessage());
        }
    }

    @GetMapping("/api/orders/{id}")
    @ResponseBody
    public Order getOrder(@PathVariable Long id) {
        return orderService.getOrderById(id).orElse(null);
    }

    @PutMapping("/api/orders/{id}/status")
    @ResponseBody
    public Order updateOrderStatus(@PathVariable Long id, @RequestParam String status) {
        Order.OrderStatus orderStatus = Order.OrderStatus.valueOf(status.toUpperCase());
        return orderService.updateOrderStatus(id, orderStatus);
    }
}
