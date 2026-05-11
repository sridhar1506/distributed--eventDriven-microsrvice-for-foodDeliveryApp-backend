package com.food.payment_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "payment_transactions")
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long orderId;
    @Column(name = "customer_id")
    private Long customerId;
    private BigDecimal amount;
    private String status;
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private String paymentMode;
    private String failureReason;

    private LocalDateTime transactionDate;
}
