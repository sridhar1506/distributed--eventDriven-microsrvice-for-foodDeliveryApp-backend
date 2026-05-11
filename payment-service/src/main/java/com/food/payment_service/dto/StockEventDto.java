package com.food.payment_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
public class StockEventDto {
    @JsonProperty("status")
    private String status;
    @JsonProperty("orderId")
    private Long orderId;
    @JsonProperty("customerId")
    private Long customerId;
    @JsonProperty("amount")
    private BigDecimal amount;
}