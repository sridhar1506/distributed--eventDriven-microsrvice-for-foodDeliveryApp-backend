package com.food.restaurant_service.Controller;

import com.food.restaurant_service.Entity.FoodItem;
import com.food.restaurant_service.Service.FoodItemService;
import com.food.restaurant_service.dto.CreateFoodRequest;
import com.food.restaurant_service.dto.FoodItemInternalResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/food")
public class FoodController {

    @Autowired
    private FoodItemService foodItemService;

    @Value("${internal.api-key}")
    private String internalApiKey;

    @PostMapping("/restaurant/{restaurantId}/category/{categoryId}")
    public ResponseEntity<FoodItem> createFoodItem(
            @jakarta.validation.Valid @RequestBody CreateFoodRequest req,
            @PathVariable Long restaurantId,
            @PathVariable Long categoryId,
            @RequestHeader("X-User-Id") Long userId) {

        return new ResponseEntity<>(foodItemService.createFoodItem(req, restaurantId, categoryId, userId),
                HttpStatus.CREATED);
    }

    @GetMapping("/restaurant/{restaurantId}")
    public ResponseEntity<List<FoodItem>> getFoodByRestaurant(@PathVariable Long restaurantId) {
        return ResponseEntity.ok(foodItemService.getFoodByRestaurantId(restaurantId));
    }

    @GetMapping("/internal/{foodId}")
    public ResponseEntity<FoodItemInternalResponse> getFoodItemByIdInternal(@PathVariable Long foodId,
            @RequestHeader(value = "X-Internal-Key", required = false) String providedKey) {

        if (internalApiKey == null || !internalApiKey.equals(providedKey)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        FoodItem food = foodItemService.getFoodItemById(foodId);

        FoodItemInternalResponse response = new FoodItemInternalResponse();
        response.setId(food.getId());
        response.setName(food.getName());
        response.setPrice(food.getPrice());
        response.setAvailable(food.isAvailable());
        response.setRestaurantOpen(food.getRestaurant().isOpen());

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{foodId}/toggle-availability")
    public ResponseEntity<FoodItem> toggleAvailability(@PathVariable Long foodId, @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(foodItemService.toggleAvailability(foodId, userId));
    }
}
