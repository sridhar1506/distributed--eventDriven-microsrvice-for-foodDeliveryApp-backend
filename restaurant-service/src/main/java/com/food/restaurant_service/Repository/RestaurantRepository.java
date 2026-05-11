package com.food.restaurant_service.Repository;

import com.food.restaurant_service.Entity.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {

    List<Restaurant> findByOwnerId(Long ownerId);
    List<Restaurant> findByNameContainingIgnoreCase(String keyword);
    java.util.Optional<Restaurant> findByNameIgnoreCase(String name);
    void deleteByOwnerId(Long ownerId);
}
