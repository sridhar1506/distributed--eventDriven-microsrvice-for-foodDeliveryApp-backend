package com.food.payment_service.controller;

import com.food.payment_service.entity.PaymentTransaction;
import com.food.payment_service.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @GetMapping("/init/{orderId}")
    public ResponseEntity<String> initializeRazorpayGateway(@PathVariable Long orderId){
        try{
            String razorpayOrderId = paymentService.createRazorpayOrder(orderId);
            return ResponseEntity.ok(razorpayOrderId);
        }catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to generate Razorpay Payment Window: " + e.getMessage());
        }
    }

    @PostMapping("/confirm/{orderId}")
    public ResponseEntity<String> confirmPayment(@PathVariable Long orderId, @RequestParam Map<String, String> payload) {
        String razorpayPaymentId = payload.get("razorpay_payment_id");
        String razorpayOrderId = payload.get("razorpay_order_id");
        String razorpaySignature = payload.get("razorpay_signature");
        String status = payload.get("status");
        String errorReason = payload.get("error_reason");
        String paymentMode = payload.get("payment_mode");

        if (status == null || !status.equals("SUCCESS")) {
            paymentService.finalizePayment(orderId, razorpayOrderId, razorpayPaymentId, "FAILED", errorReason, paymentMode);
            return ResponseEntity.badRequest().body("Payment Failed: " + (errorReason != null ? errorReason : "Unknown error. Please try again."));
        }

        if (razorpaySignature == null || razorpaySignature.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid Payment Signature!");
        }

        paymentService.finalizePayment(orderId, razorpayOrderId, razorpayPaymentId, "SUCCESS", null, paymentMode);
        return ResponseEntity.ok("Payment Processed Securely for Order #" + orderId);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PaymentTransaction>> getUserTransactions(@PathVariable Long userId) {
        return ResponseEntity.ok(paymentService.getUserTransactions(userId));
    }
}

