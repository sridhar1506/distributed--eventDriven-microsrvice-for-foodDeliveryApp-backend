package com.food.order_service.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateOrderRequest {
    private Long restaurantId;
    private String deliveryAddress;
}
