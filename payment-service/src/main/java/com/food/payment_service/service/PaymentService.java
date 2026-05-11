package com.food.payment_service.service;

import com.food.payment_service.dto.PaymentEventDto;
import com.food.payment_service.entity.PaymentTransaction;
import com.food.payment_service.repository.TransactionRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PaymentService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    // We create a "Pending" record in our database before the user actually pays so
    // we can track the attempt.
    @Transactional
    public void createPendingTransaction(Long orderId, Long customerId, BigDecimal amount) {
        if (transactionRepository.findByOrderId(orderId).isPresent()) {
            return;
        }
        PaymentTransaction tx = new PaymentTransaction();
        tx.setOrderId(orderId);
        tx.setCustomerId(customerId);
        tx.setAmount(amount);

        System.out.println("DEBUG [Payment Service]: Saving Transaction for Order #" + orderId
                + " - Customer ID in Entity: " + tx.getCustomerId());

        tx.setStatus("PENDING");
        tx.setTransactionDate(LocalDateTime.now());
        transactionRepository.save(tx);
    }

    // This method creates a specific order ID in the Razorpay system so the user
    // can pay securely.
    @Transactional
    public String createRazorpayOrder(Long orderId) throws Exception {
        PaymentTransaction tx = transactionRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        RazorpayClient razorpay = new RazorpayClient(razorpayKeyId, razorpayKeySecret);

        // We convert the price to "Paise" (cents) because that's what Razorpay expects.
        int amountInPaise = tx.getAmount().multiply(new BigDecimal("100")).setScale(0, RoundingMode.HALF_UP).intValue();

        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", amountInPaise);
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", "txn_" + orderId);

        if (tx.getRazorpayOrderId() != null) {
            return tx.getRazorpayOrderId();
        }

        Order razorpayOrder = razorpay.orders.create(orderRequest);
        String generatedRazorpayOrderId = razorpayOrder.get("id");
        tx.setRazorpayOrderId(generatedRazorpayOrderId);
        transactionRepository.save(tx);

        return generatedRazorpayOrderId;
    }

    // Once the payment is done, we update our database and tell the Order service
    // that it's okay to proceed.
    @Transactional
    public void finalizePayment(Long orderId, String razorpayOrderId, String paymentId, String status,
            String failureReason, String paymentMode) {
        PaymentTransaction tx = transactionRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Transaction not found for Order #" + orderId));

        tx.setRazorpayOrderId(razorpayOrderId);
        tx.setRazorpayPaymentId(paymentId);
        tx.setPaymentMode(paymentMode);

        if ("SUCCESS".equalsIgnoreCase(status)) {
            tx.setStatus("SUCCESS");
            transactionRepository.save(tx);

            // We notify the rest of the system that the payment was successful.
            PaymentEventDto event = new PaymentEventDto();
            event.setOrderId(orderId);
            event.setStatus("SUCCESS");
            kafkaTemplate.send("payment-topic", event);

        } else {
            tx.setStatus("FAILED");
            tx.setFailureReason(failureReason);
            transactionRepository.save(tx);
        }
    }

    public List<PaymentTransaction> getUserTransactions(Long customerId) {
        return transactionRepository.findByCustomerId(customerId);
    }

    @KafkaListener(topics = "delete-user-topic", groupId = "payment-group")
    @Transactional
    public void deleteUserTransactions(Long userId) {
        transactionRepository.deleteByCustomerId(userId);
    }
}
