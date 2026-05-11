package com.food.order_service.controller;

import com.food.order_service.dto.AddCartItemRequest;
import com.food.order_service.service.CartService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import com.food.order_service.dto.CartDto;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    @Autowired
    private CartService cartService;

    @PostMapping("/add")
    @Transactional
    public ResponseEntity<CartDto> addItem(@Valid @RequestBody AddCartItemRequest req,
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(cartService.addItemToCart(req, userId));
    }

    @GetMapping
    public ResponseEntity<List<CartDto>> getUserCarts(@RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(cartService.getAllUserCarts(userId));
    }

    @DeleteMapping("/{cartId}")
    public ResponseEntity<String> clearCart(@PathVariable Long cartId) {
        cartService.clearCart(cartId);
        return ResponseEntity.ok("Cart cleared Successfully");
    }

    @DeleteMapping("/all")
    public ResponseEntity<String> clearAllCarts(@RequestHeader("X-User-Id") Long userId) {
        cartService.clearAllCarts(userId);
        return ResponseEntity.ok("All carts cleared Successfully");
    }

    @PutMapping("/item/{itemId}/update")
    public ResponseEntity<CartDto> updateItemQuantity(
            @PathVariable Long itemId,
            @RequestParam int quantity,
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(cartService.updateCartItemQuantity(itemId, quantity, userId));
    }
}
