package com.food.order_service.repository;

import com.food.order_service.entity.OrderEntity;

import org.springframework.data.repository.query.Param;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, Long> {

    @Query("SELECT o FROM OrderEntity o JOIN FETCH o.items WHERE o.customerId = :customerId")
    List<OrderEntity> findByCustomerId(@Param("customerId") Long customerId);

    @Query("SELECT o FROM OrderEntity o JOIN FETCH o.items WHERE o.restaurantId = :restaurantId")
    List<OrderEntity> findByRestaurantId(@Param("restaurantId") Long restaurantId);

    void deleteByCustomerId(Long customerId);
}
