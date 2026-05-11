package com.food.restaurant_service.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonProperty;

@Getter
@Setter
public class FoodItemInternalResponse {

    private Long id;
    private String name;
    private BigDecimal price;
    @JsonProperty("available")
    private boolean available;
    private boolean restaurantOpen;

}
