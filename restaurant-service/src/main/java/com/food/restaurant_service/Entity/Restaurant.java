package com.food.restaurant_service.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "restaurants")
@NoArgsConstructor
@AllArgsConstructor
public class Restaurant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long ownerId; //User ID from auth-service

    private String name;
    private String description;
    private String cuisineType;
    private String imageUrl;
    private String openingHours;
    private boolean open;

    @OneToMany(mappedBy = "restaurant", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FoodItem> foodItems = new ArrayList<>();

}
