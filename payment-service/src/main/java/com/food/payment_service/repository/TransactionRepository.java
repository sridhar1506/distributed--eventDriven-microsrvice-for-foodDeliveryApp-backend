package com.food.payment_service.repository;

import com.food.payment_service.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    Optional<PaymentTransaction> findByOrderId(Long orderId);

    List<PaymentTransaction> findByCustomerId(Long customerId);

    void deleteByCustomerId(Long customerId);
}
