package com.food.order_service.dto;

import lombok.Getter;
import lombok.Setter;
@Getter
@Setter
public class PaymentEventDto {
    private Long orderId;
    private String status; // "SUCCESS" or "FAILED"
}
