package com.food.order_service.service;

import com.food.order_service.dto.FoodItemDto;
import com.food.order_service.dto.RestaurantDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

// This class listens for messages from other services so we can keep our local data in sync.
@Service
public class CartMetadataConsumer {

    @Autowired
    private CartService cartService;

    // This method runs whenever a restaurant name is updated in the system.
    @KafkaListener(topics = "restaurant-metadata-topic", groupId = "order-group")
    public void handleRestaurantUpdate(RestaurantDto restaurantDto) {
        System.out.println("Updating local database: Restaurant " + restaurantDto.getName());
        cartService.updateRestaurantName(restaurantDto.getId(), restaurantDto.getName());
    }

    // This method runs whenever a food item name is updated in the system.
    @KafkaListener(topics = "food-item-metadata-topic", groupId = "order-group")
    public void handleFoodItemUpdate(FoodItemDto foodItemDto) {
        System.out.println("Updating local database: Food Item " + foodItemDto.getName());
        cartService.updateFoodName(foodItemDto.getId(), foodItemDto.getName());
    }
}
