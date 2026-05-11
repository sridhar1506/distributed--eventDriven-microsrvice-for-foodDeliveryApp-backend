package com.food.order_service.repository;

import com.food.order_service.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
    List<OrderEntity> findByCustomerId(Long customerId);

    List<OrderEntity> findByRestaurantId(Long restaurantId);

    void deleteByCustomerId(Long customerId);
}
