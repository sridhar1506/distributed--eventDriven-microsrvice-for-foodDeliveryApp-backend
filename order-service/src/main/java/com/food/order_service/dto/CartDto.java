package com.food.order_service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CartDto {
    private Long id;
    private Long customerId;
    private Long restaurantId;
    private String restaurantName;
    private BigDecimal totalAmount;
    private List<CartItemDto> items = new ArrayList<>();

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CartItemDto {
        private Long id;
        private Long foodItemId;
        private String foodName;
        private int quantity;
        private BigDecimal totalPrice;
    }
}
