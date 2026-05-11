package com.food.order_service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddCartItemRequest {

    @NotNull
    private Long foodItemId;

    @NotNull
    private Long restaurantId;

    @Min(value = 1)
    private int quantity;
}
