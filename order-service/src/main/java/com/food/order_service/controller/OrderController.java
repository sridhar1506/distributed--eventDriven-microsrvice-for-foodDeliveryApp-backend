package com.food.order_service.controller;

import com.food.order_service.dto.CreateOrderRequest;
import com.food.order_service.entity.OrderEntity;
import com.food.order_service.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @org.springframework.beans.factory.annotation.Value("${internal.api-key}")
    private String internalApiKey;

    @PostMapping
    public ResponseEntity<OrderEntity> placeOrder(@Valid @RequestBody CreateOrderRequest req,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role) {
        
        if (role == null || !role.contains("ROLE_CUSTOMER")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrder(req, userId));
    }

    @GetMapping("/restaurant/{restaurantId}")
    public ResponseEntity<List<OrderEntity>> getRestaurantOrders(@PathVariable Long restaurantId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role) {
            
        if (role == null || !role.contains("ROLE_RESTAURANT_OWNER")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(orderService.getRestaurantOrders(restaurantId, userId));
    }

    @PutMapping("/{orderId}/owner-status")
    public ResponseEntity<OrderEntity> updateOrderStatusByOwner(@PathVariable Long orderId,
            @RequestParam String status, 
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role) {
            
        if (role == null || !role.contains("ROLE_RESTAURANT_OWNER")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(orderService.updateOrderStatusByOwner(orderId, status, userId));
    }

    @PutMapping("/{orderId}/status")
    public ResponseEntity<String> updateStatus(
            @PathVariable Long orderId,
            @RequestParam String status,
            @RequestHeader(value = "X-Internal-Key", required = false) String providedKey) {

        if (internalApiKey == null || !internalApiKey.equals(providedKey)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Unauthorized internal access");
        }

        orderService.updateOrderStatus(orderId, status);
        return ResponseEntity.ok("Order Status updated to: " + status);
    }

    @GetMapping("/user")
    public ResponseEntity<List<OrderEntity>> getUserOrders(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role,
            @RequestHeader(value = "X-Internal-Key", required = false) String providedKey) {
            
        if (role == null || internalApiKey == null || !role.contains("ROLE_CUSTOMER") || !internalApiKey.equals(providedKey)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(orderService.getUserOrders(userId));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderEntity> getOrderById(@PathVariable Long orderId, 
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role) {
            
        // We allow both Customers and Owners to see a specific order,
        // as the service already checks if they are the Customer who placed it OR the Restaurant Owner.
        if (role == null || (!role.contains("ROLE_CUSTOMER") && !role.contains("ROLE_RESTAURANT_OWNER"))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(orderService.getOrderById(orderId, userId));
    }
}
