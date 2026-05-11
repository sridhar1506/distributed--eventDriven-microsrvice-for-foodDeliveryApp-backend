package com.food.order_service.repository;

import com.food.order_service.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
    List<OutboxEvent> findByStatus(String status);
    void deleteByAggregateId(String aggregateId);
}
