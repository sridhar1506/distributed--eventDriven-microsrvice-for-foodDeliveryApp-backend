package com.food.api_gateway.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping(value = "/auth", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> authFallback() {
        return ResponseEntity.status(503).body(Map.of("message", "Authentication service is temporarily unavailable. Please try again."));
    }

    @PostMapping(value = "/auth", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> authFallbackPost() {
        return ResponseEntity.status(503).body(Map.of("message", "Authentication service is temporarily unavailable. Please try again."));
    }

    @GetMapping(value = "/restaurant", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> restaurantFallback() {
        return ResponseEntity.status(503).body(Map.of("message", "Restaurant service is temporarily unavailable. Please try again shortly."));
    }

    @PostMapping(value = "/restaurant", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> restaurantFallbackPost() {
        return ResponseEntity.status(503).body(Map.of("message", "Restaurant service is temporarily unavailable. Please try again shortly."));
    }

    @GetMapping(value = "/order", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> orderFallback() {
        return ResponseEntity.status(503).body(Map.of("message", "The ordering system is busy. Your cart is safe—please try again in a moment."));
    }

    @PostMapping(value = "/order", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> orderFallbackPost() {
        return ResponseEntity.status(503).body(Map.of("message", "The ordering system is busy. Your cart is safe—please try again in a moment."));
    }

    @GetMapping(value = "/payment", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> paymentFallback() {
        return ResponseEntity.status(503).body(Map.of("message", "Payment service is experiencing delays. Please wait a moment before trying again."));
    }

    @PostMapping(value = "/payment", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> paymentFallbackPost() {
        return ResponseEntity.status(503).body(Map.of("message", "Payment service is experiencing delays. Please wait a moment before trying again."));
    }
}
