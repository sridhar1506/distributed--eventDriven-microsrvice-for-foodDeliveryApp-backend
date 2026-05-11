package com.food.payment_service.service;

import com.food.payment_service.dto.StockEventDto;
import org.springframework.beans.factory.annotation.Autowired;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.api.trace.Span;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

// This class listens for messages about the food stock being ready so we can start the payment process.
@Service
public class SagaPaymentListener {

    @Autowired
    private PaymentService paymentService;
    @Autowired
    private ObjectMapper objectMapper;

    // This method runs when the Restaurant service confirms that the food is reserved.
    @KafkaListener(topics = "stock-topic", groupId = "payment-group")
    public void handleStockEvent(StockEventDto stockEvent) {
        System.out.println("LOG [Payment Service]: Stock Event Received for Order #" + stockEvent.getOrderId() + " - Status: " + stockEvent.getStatus() + ", Customer: " + stockEvent.getCustomerId());
        
        try {
            // Tag incoming message for visibility in Jaeger
            String incomingJson = objectMapper.writeValueAsString(stockEvent);
            Span.current().setAttribute("kafka.payload", incomingJson);
        } catch (Exception e) {
            System.err.println("Failed to tag incoming Kafka payload: " + e.getMessage());
        }
        
        if ("RESERVED".equals(stockEvent.getStatus())) {
            // We create a "pending" payment record once the kitchen is ready.
            paymentService.createPendingTransaction(stockEvent.getOrderId(), stockEvent.getCustomerId(), stockEvent.getAmount());
        }
    }
}
