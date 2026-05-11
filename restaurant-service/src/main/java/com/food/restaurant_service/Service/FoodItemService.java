package com.food.restaurant_service.Service;

import com.food.restaurant_service.Entity.Category;
import com.food.restaurant_service.Entity.FoodItem;
import com.food.restaurant_service.Entity.Restaurant;
import com.food.restaurant_service.Repository.FoodItemRepository;
import com.food.restaurant_service.dto.CreateFoodRequest;
import com.food.restaurant_service.dto.FoodItemInternalResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@Service
public class FoodItemService {

    @Autowired
    private FoodItemRepository foodItemRepository;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private RestaurantService restaurantService;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    public FoodItem createFoodItem(CreateFoodRequest req, Long restaurantId, Long categoryId, Long userId) {
        if (req.getPrice() == null || req.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Valid price is strictly required");
        }
        Restaurant restaurant = restaurantService.getRestaurantById(restaurantId);

        if (!restaurant.getOwnerId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You are not authorised to create items in this restaurant!");
        }
        Category category = categoryService.getCategoryById(categoryId);

        if (foodItemRepository.findByRestaurantIdAndNameIgnoreCase(restaurantId, req.getName()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A food item with this name already exists in your restaurant menu.");
        }

        FoodItem foodItem = new FoodItem();
        foodItem.setName(req.getName());
        foodItem.setDescription(req.getDescription());
        foodItem.setPrice(req.getPrice());
        foodItem.setVegetarian(req.isVegetarian());
        foodItem.setImageUrl(req.getImageUrl());
        foodItem.setAvailable(true);
        foodItem.setRestaurant(restaurant);
        foodItem.setCategory(category);
        FoodItem saved = foodItemRepository.save(foodItem);

        // We notify other services that a new food item has been added to the menu.
        try {
            FoodItemInternalResponse event = new FoodItemInternalResponse();
            event.setId(saved.getId());
            event.setName(saved.getName());
            event.setPrice(saved.getPrice());
            event.setAvailable(saved.isAvailable());

            // We reuse an existing class to send this update across the system.
            kafkaTemplate.send("food-item-metadata-topic", event);
        } catch (Exception e) {
            System.err.println("Could not tell other services about the new food item: " + e.getMessage());
        }

        return saved;
    }

    public FoodItem getFoodItemById(Long id) {
        return foodItemRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Food not found"));
    }

    public List<FoodItem> getFoodByRestaurantId(Long restaurantId) {
        return foodItemRepository.findByRestaurantId(restaurantId);
    }

    public FoodItem toggleAvailability(Long foodId, Long userId) {
        FoodItem foodItem = getFoodItemById(foodId);
        
        // Security Check: Only the owner of the restaurant can toggle stock
        if (!foodItem.getRestaurant().getOwnerId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
                "You are not authorised to change stock for this item!");
        }

        if (foodItem.isAvailable()) {
            foodItem.setAvailable(false);
        } else {
            foodItem.setAvailable(true);
        }
        return foodItemRepository.save(foodItem);
    }
}
