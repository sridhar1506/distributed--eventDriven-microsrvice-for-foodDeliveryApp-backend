package com.food.restaurant_service.Service;

import com.food.restaurant_service.Entity.FoodItem;
import com.food.restaurant_service.Entity.Restaurant;
import com.food.restaurant_service.Repository.FoodItemRepository;
import com.food.restaurant_service.Repository.RestaurantRepository;
import com.food.restaurant_service.dto.CreateRestaurantRequest;
import com.food.restaurant_service.dto.OrderEventDto;
import com.food.restaurant_service.dto.RestaurantDto;
import com.food.restaurant_service.dto.StockEventDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.annotation.KafkaListener;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.api.trace.Span;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
public class RestaurantService {

    @Autowired
    private RestaurantRepository restaurantRepository;
    @Autowired
    private FoodItemRepository foodItemRepository;
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;
    @Autowired
    private ObjectMapper objectMapper;

    // This method handles new orders coming in from the Order service.
    @KafkaListener(topics = "order-topic", groupId = "restaurant-group")
    public void handleOrderEvent(OrderEventDto orderEvent) {

        System.out.println("LOG [Restaurant Service]: Order Received for Order #" + orderEvent.getOrderId()
                + " - Customer: " + orderEvent.getCustomerId());

        try {
            // Tag incoming message for visibility in Jaeger
            String incomingJson = objectMapper.writeValueAsString(orderEvent);
            Span.current().setAttribute("kafka.payload.incoming", incomingJson);
        } catch (Exception e) {
            System.err.println("Failed to tag incoming Kafka payload: " + e.getMessage());
        }

        System.out.println("The system is now checking if we have enough stock and if the restaurant is open.");

        try {

            Optional<Restaurant> restaurant = restaurantRepository.findById(orderEvent.getRestaurantId());
            boolean allItemsValid = true;

            if (restaurant.isPresent()) {
                if (!restaurant.get().isOpen()) {
                    allItemsValid = false;
                } else {
                    for (OrderEventDto.OrderItemDto item : orderEvent.getItems()) {
                        Optional<FoodItem> foodItem = foodItemRepository.findById(item.getFoodItemId());
                        if (foodItem.isEmpty()
                                || !foodItem.get().getRestaurant().getId().equals(orderEvent.getRestaurantId())
                                || !foodItem.get().isAvailable()) {
                            allItemsValid = false;
                            break;
                        }
                    }
                }
            } else {
                allItemsValid = false;
            }

            StockEventDto stockEvent = new StockEventDto();
            stockEvent.setOrderId(orderEvent.getOrderId());
            stockEvent.setCustomerId(orderEvent.getCustomerId());
            stockEvent.setAmount(orderEvent.getTotalAmount());

            if (allItemsValid) {
                System.out.println("Kitchen is ready for Order #" + orderEvent.getOrderId());
                stockEvent.setStatus("RESERVED");
            } else {
                stockEvent.setStatus("REJECTED");
            }

            System.out.println("LOG [Restaurant Service]: Stock " + stockEvent.getStatus() + " for Order #"
                    + orderEvent.getOrderId() + " - Customer: " + stockEvent.getCustomerId()
                    + ". Notifying Payment Service...");

            try {
                // Tag outgoing message for visibility in Jaeger
                String outgoingJson = objectMapper.writeValueAsString(stockEvent);
                Span.current().setAttribute("kafka.payload.outgoing", outgoingJson);
            } catch (Exception e) {
                System.err.println("Failed to tag outgoing Kafka payload: " + e.getMessage());
            }

            kafkaTemplate.send("stock-topic", stockEvent);
        } catch (Exception e) {
            System.err.println("Error while checking the kitchen status: " + e.getMessage());
            throw e;
        }
    }

    public Restaurant toggleOpenStatus(Long id, Long userId) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant not found"));
        if (!restaurant.getOwnerId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You are not authorised to change this restaurant's status!");
        }
        restaurant.setOpen(!restaurant.isOpen());
        return restaurantRepository.save(restaurant);
    }

    public List<Restaurant> searchByName(String name) {
        return restaurantRepository.findByNameContainingIgnoreCase(name);
    }

    public Restaurant createRestaurant(CreateRestaurantRequest req) {
        if (restaurantRepository.findByNameIgnoreCase(req.getName()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Restaurant name already exists");
        }

        Restaurant restaurant = new Restaurant();
        restaurant.setOwnerId(req.getOwnerID());
        restaurant.setName(req.getName());
        restaurant.setDescription(req.getDescription());
        restaurant.setCuisineType(req.getCuisineType());
        restaurant.setOpeningHours(req.getOpeningHours());
        restaurant.setOpen(true);
        Restaurant saved = restaurantRepository.save(restaurant);

        // We tell other services that a new restaurant has been created so they can
        // update their local copies.
        try {
            RestaurantDto event = new RestaurantDto(saved.getId(), saved.getName());
            kafkaTemplate.send("restaurant-metadata-topic", event);
        } catch (Exception e) {
            System.err.println("Failed to notify other services about the new restaurant: " + e.getMessage());
        }

        return saved;
    }

    @Transactional
    public void deleteRestaurant(Long id, Long ownerId) {
        Restaurant restaurant = getRestaurantById(id);
        if (!restaurant.getOwnerId().equals(ownerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not own this restaurant");
        }
        restaurantRepository.delete(restaurant);
    }

    @Transactional
    @KafkaListener(topics = "delete-user-topic", groupId = "restaurant-group")
    public void deleteByOwnerId(Long ownerId) {
        restaurantRepository.deleteByOwnerId(ownerId);
    }

    public boolean isOwner(Long restaurantId, Long userId) {
        Optional<Restaurant> restaurant = restaurantRepository.findById(restaurantId);
        return restaurant.isPresent() && restaurant.get().getOwnerId().equals(userId);
    }

    @Transactional(readOnly = true)
    public Restaurant getRestaurantById(Long id) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant not found"));
        // We pre-load the lists of food to avoid errors later.
        restaurant.getFoodItems().size();
        return restaurant;
    }

    public List<Restaurant> getAllRestaurants() {
        List<Restaurant> restaurants = restaurantRepository.findAll();
        restaurants.forEach(r -> {
            r.getFoodItems().size();
        });
        return restaurants;
    }

    @Transactional(readOnly = true)
    public List<Restaurant> getRestaurantByOwnerId(Long ownerId) {
        List<Restaurant> restaurants = restaurantRepository.findByOwnerId(ownerId);
        restaurants.forEach(r -> {
            r.getFoodItems().size();
        });
        return restaurants;
    }
}
