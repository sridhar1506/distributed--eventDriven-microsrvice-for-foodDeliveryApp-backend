package com.food.restaurant_service.Controller;

import com.food.restaurant_service.Entity.Restaurant;
import com.food.restaurant_service.Service.RestaurantService;
import com.food.restaurant_service.dto.CreateRestaurantRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import jakarta.validation.Valid;

// This controller handles all requests related to restaurants, such as finding them, creating them, or updating their status.
@RestController
@RequestMapping("/api/restaurants")
public class RestaurantController {

    @Autowired
    private RestaurantService restaurantService;

    @Value("${internal.api-key}")
    private String internalApiKey;

    // A restaurant owner can use this to create a new profile for their restaurant.
    @PostMapping("/create")
    public ResponseEntity<Restaurant> createRestaurant(
            @Valid @RequestBody CreateRestaurantRequest req,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {

        // We check if the user is actually an owner before letting them proceed.
        if (role == null || !role.contains("ROLE_RESTAURANT_OWNER")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        req.setOwnerID(userId);
        if (req.getOwnerID() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return new ResponseEntity<>(restaurantService.createRestaurant(req), HttpStatus.CREATED);
    }

    @PutMapping("/{id}/toggle-status")
    public ResponseEntity<Restaurant> toggleStatus(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestHeader(value = "X-User-Id") Long userId) {

        if (role == null || !role.contains("ROLE_RESTAURANT_OWNER")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Restaurant updated = restaurantService.toggleOpenStatus(id, userId);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/search")
    public ResponseEntity<List<Restaurant>> searchRestaurants(@RequestParam String keyword) {
        return ResponseEntity.ok(restaurantService.searchByName(keyword));
    }

    @GetMapping("/{restaurantId}/check-owner")
    public ResponseEntity<Boolean> isOwner(@PathVariable Long restaurantId, @RequestParam Long userId) {
        return ResponseEntity.ok(restaurantService.isOwner(restaurantId, userId));
    }

    @GetMapping
    public ResponseEntity<List<Restaurant>> getAllRestaurants(@RequestHeader(value = "X-Internal-Key", required = false) String providedKey) {
        if(providedKey == null || !providedKey.equals(internalApiKey)){
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(restaurantService.getAllRestaurants());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Restaurant> getRestaurantById(@PathVariable Long id) {
        return ResponseEntity.ok(restaurantService.getRestaurantById(id));
    }

    @GetMapping("/owner/{ownerId}")
    public ResponseEntity<List<Restaurant>> getRestaurantByOwnerId(@PathVariable Long ownerId) {
        return ResponseEntity.ok(restaurantService.getRestaurantByOwnerId(ownerId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRestaurant(@PathVariable Long id, @RequestHeader("X-User-Id") Long userId) {
        restaurantService.deleteRestaurant(id, userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/owner/{ownerId}")
    public ResponseEntity<Void> deleteRestaurantsByOwner(@PathVariable Long ownerId) {
        restaurantService.deleteByOwnerId(ownerId);
        return ResponseEntity.noContent().build();
    }
}
