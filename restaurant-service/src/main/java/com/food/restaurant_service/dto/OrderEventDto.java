package com.food.restaurant_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.List;
@Getter
@Setter
public class OrderEventDto {
    @JsonProperty("orderId")
    private Long orderId;
    @JsonProperty("restaurantId")
    private Long restaurantId;
    @JsonProperty("customerId")
    private Long customerId;
    @JsonProperty("totalAmount")
    private BigDecimal totalAmount;
    @JsonProperty("deliveryAddress")
    private String deliveryAddress;
    @JsonProperty("items")
    private List<OrderItemDto> items;

    @Getter
    @Setter
    public static class OrderItemDto {
        @JsonProperty("foodItemId")
        private Long foodItemId;
        @JsonProperty("foodName")
        private String foodName;
        @JsonProperty("quantity")
        private Integer quantity;
    }
}