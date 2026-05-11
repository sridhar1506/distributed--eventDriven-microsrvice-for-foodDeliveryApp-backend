package com.food.restaurant_service.Repository;

import com.food.restaurant_service.Entity.FoodItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FoodItemRepository extends JpaRepository<FoodItem, Long> {

    List<FoodItem> findByRestaurantId(Long restaurantId);
    List<FoodItem> findByCategoryId(Long categoryId);
    Optional<FoodItem> findByRestaurantIdAndNameIgnoreCase(Long restaurantId, String name);
}
