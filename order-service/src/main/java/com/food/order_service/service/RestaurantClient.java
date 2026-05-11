package com.food.order_service.service;

import com.food.order_service.dto.FoodItemDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import java.util.Map;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "restaurant-service")
public interface RestaurantClient {

    @GetMapping("/api/food/internal/{foodId}")
    FoodItemDto getFoodItemByInternal(@PathVariable("foodId") Long foodId,
            @RequestHeader("X-Internal-Key") String internalKey);

    @GetMapping("/api/restaurants/{id}")
    Map<String, Object> getRestaurantById(@PathVariable("id") Long id);

    @GetMapping("/api/restaurants/{restaurantId}/check-owner")
    Boolean isOwner(@PathVariable("restaurantId") Long restaurantId, @RequestParam("userId") Long userId);
}
