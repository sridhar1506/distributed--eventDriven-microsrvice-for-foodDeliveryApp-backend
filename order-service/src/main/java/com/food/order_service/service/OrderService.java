package com.food.order_service.service;

import com.food.order_service.dto.CreateOrderRequest;
import com.food.order_service.dto.OrderEventDto;
import com.food.order_service.dto.PaymentEventDto;
import com.food.order_service.dto.StockEventDto;
import com.food.order_service.entity.*;
import com.food.order_service.repository.CartRepository;
import com.food.order_service.repository.OrderRepository;
import com.food.order_service.repository.OutboxEventRepository;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private CartRepository cartRepository;
    @Autowired
    private OutboxEventRepository outboxEventRepository;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private OpenTelemetry openTelemetry;
    @Autowired
    private RestaurantClient restaurantClient;

    @Transactional
    public OrderEntity createOrder(CreateOrderRequest req, Long userId) {

        Cart cart = cartRepository.findByCustomerIdAndRestaurantId(userId, req.getRestaurantId());
        if (cart == null || cart.getItems().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Empty Cart");
        }

        OrderEntity order = new OrderEntity();
        order.setCustomerId(userId);
        order.setRestaurantId(req.getRestaurantId());
        order.setTotalAmount(cart.getTotalAmount());
        order.setOrderStatus("PENDING_PAYMENT");
        order.setDeliveryAddress(req.getDeliveryAddress());
        order.setCreatedAt(new Date());

        OrderEventDto eventDto = new OrderEventDto();
        eventDto.setCustomerId(userId);
        eventDto.setRestaurantId(req.getRestaurantId());
        eventDto.setTotalAmount(cart.getTotalAmount());
        eventDto.setDeliveryAddress(order.getDeliveryAddress());
        eventDto.setItems(new ArrayList<>());

        for (CartItem ci : cart.getItems()) {
            OrderItem oi = new OrderItem();
            oi.setFoodItemId(ci.getFoodItemID());
            oi.setFoodName(ci.getFoodName());
            oi.setQuantity(ci.getQuantity());
            oi.setTotalPrice(ci.getTotalPrice());
            oi.setOrder(order);
            order.getItems().add(oi);
            OrderEventDto.OrderItemDto itemDto = new OrderEventDto.OrderItemDto();
            itemDto.setFoodItemId(ci.getFoodItemID());
            itemDto.setFoodName(ci.getFoodName());
            itemDto.setQuantity(ci.getQuantity());
            eventDto.getItems().add(itemDto);
        }

        OrderEntity savedOrder = orderRepository.save(order);

        eventDto.setOrderId(savedOrder.getId());

        System.out.println("DEBUG [Order Service]: Creating Outbox Event for Order #" + savedOrder.getId()
                + " - Customer: " + userId);

        try {
            String payloadJson = objectMapper.writeValueAsString(eventDto);
            // Capture current Trace Context for the Outbox background task
            StringBuilder traceParentBuilder = new StringBuilder();
            openTelemetry.getPropagators().getTextMapPropagator().inject(Context.current(), traceParentBuilder,
                    (carrier, key, value) -> {
                        if ("traceparent".equals(key)) {
                            carrier.append(value);
                        }
                    });

            OutboxEvent outboxEvent = new OutboxEvent(
                    savedOrder.getId().toString(),
                    "order-topic",
                    payloadJson,
                    traceParentBuilder.toString());

            // Tag the current trace Span with the Kafka payload for visibility in Jaeger
            Span.current().setAttribute("kafka.payload", payloadJson);

            outboxEventRepository.save(outboxEvent);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Event Serialization Failed");
        }
        return savedOrder;
    }

    @KafkaListener(topics = "payment-topic", groupId = "order-group")
    public void handlePaymentResult(PaymentEventDto paymentDto) {
        if ("SUCCESS".equalsIgnoreCase(paymentDto.getStatus())) {

            System.out.println("LOG [Order Service]: Payment received for Order #" + paymentDto.getOrderId());
            updateOrderStatus(paymentDto.getOrderId(), "PAID");
        }
    }

    @KafkaListener(topics = "stock-topic", groupId = "order-stock-group")
    @Transactional
    public void handleStockResult(StockEventDto stockEvent) {
        if ("REJECTED".equals(stockEvent.getStatus())) {

            OrderEntity order = orderRepository.findById(stockEvent.getOrderId()).orElse(null);

            if (order != null) {
                order.setOrderStatus("REJECTED_BY_RESTAURANT");
                orderRepository.save(order);

                outboxEventRepository.deleteByAggregateId(stockEvent.getOrderId().toString());
            }
        }
    }

    public List<OrderEntity> getRestaurantOrders(Long restaurantId, Long userId) {
        Boolean isOwner = restaurantClient.isOwner(restaurantId, userId);
        if (Boolean.FALSE.equals(isOwner)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not own this restaurant");
        }
        return orderRepository.findByRestaurantId(restaurantId);
    }

    public OrderEntity updateOrderStatusByOwner(Long orderId, String status, Long userId) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order Not Found"));
        Boolean isOwner = restaurantClient.isOwner(order.getRestaurantId(), userId);
        if (Boolean.FALSE.equals(isOwner)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You are not authorised to change this order's status!");
        }
        order.setOrderStatus(status);
        return orderRepository.save(order);
    }

    public void updateOrderStatus(Long orderId, String status) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order Not Found"));
        order.setOrderStatus(status);
        orderRepository.save(order);

        if ("PAID".equalsIgnoreCase(status)) {
            Cart cart = cartRepository.findByCustomerIdAndRestaurantId(order.getCustomerId(), order.getRestaurantId());
            if (cart != null) {
                cart.getItems().clear();
                cart.setTotalAmount(BigDecimal.ZERO);
                cartRepository.save(cart);
            }
        }
    }

    public List<OrderEntity> getUserOrders(Long userId) {
        return orderRepository.findByCustomerId(userId);
    }

    public OrderEntity getOrderById(Long orderId, Long userId) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order Not Found"));

        // SECURITY CHECK: Only the Customer who placed it or the Restaurant Owner can
        // view it
        boolean isCustomer = order.getCustomerId().equals(userId);
        boolean isOwner = restaurantClient.isOwner(order.getRestaurantId(), userId);

        if (!isCustomer && !isOwner) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorised to view this order!");
        }

        return order;
    }

    @KafkaListener(topics = "delete-user-topic", groupId = "order-group")
    @Transactional
    public void deleteOrdersByUser(Long userId) {
        orderRepository.deleteByCustomerId(userId);
    }
}
