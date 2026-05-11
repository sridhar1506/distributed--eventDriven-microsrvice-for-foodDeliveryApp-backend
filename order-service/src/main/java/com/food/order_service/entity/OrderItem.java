package com.food.order_service.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
public class OrderItem {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne @JoinColumn(name = "order_id") @JsonIgnore
    private OrderEntity order;

    private Long foodItemId;
    private String foodName;
    private int quantity;
    private BigDecimal totalPrice;

}
