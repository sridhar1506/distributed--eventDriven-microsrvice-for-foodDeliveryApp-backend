package com.food.order_service.repository;

import com.food.order_service.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    Cart findByCustomerIdAndRestaurantId(Long userId, Long restaurantId);

    List<Cart> findAllByCustomerId(Long userId);
    
    List<Cart> findAllByRestaurantId(Long restaurantId);

    Cart findByCustomerId(Long id);
}
