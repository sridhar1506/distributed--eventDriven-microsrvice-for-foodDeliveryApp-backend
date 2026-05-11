package com.food.restaurant_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateRestaurantRequest {

    private Long ownerID;

    @NotBlank(message = "Restaurant name cannot be empty")
    private String name;

    private String description;

    @NotBlank(message = "Cuisine type is required")
    private String cuisineType;

    private String openingHours;

}
