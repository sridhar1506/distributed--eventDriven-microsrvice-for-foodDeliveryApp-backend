package com.food.order_service.service;

import com.food.order_service.dto.AddCartItemRequest;
import com.food.order_service.dto.FoodItemDto;
import com.food.order_service.entity.Cart;
import com.food.order_service.entity.CartItem;
import com.food.order_service.repository.CartRepository;
import com.food.order_service.repository.CartItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.food.order_service.dto.CartDto;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class CartService {

    @Autowired
    private CartRepository cartRepository;
    @Autowired
    private CartItemRepository cartItemRepository;
    @Autowired
    private RestaurantClient restaurantClient;

    @Value("${internal.api-key}")
    private String internalApiKey;

    // This converts our database information into a package that the frontend can
    // understand.
    private CartDto mapToDto(Cart cart) {
        CartDto dto = new CartDto();
        dto.setId(cart.getId());
        dto.setCustomerId(cart.getCustomerId());
        dto.setRestaurantId(cart.getRestaurantId());
        dto.setRestaurantName(cart.getRestaurantName()); // Use the local copy of the name to keep things fast
        dto.setTotalAmount(cart.getTotalAmount());

        for (CartItem item : cart.getItems()) {
            CartDto.CartItemDto itemDto = new CartDto.CartItemDto();
            itemDto.setId(item.getId());
            itemDto.setFoodItemId(item.getFoodItemID());
            itemDto.setFoodName(item.getFoodName()); // Use the local copy of the food name
            itemDto.setQuantity(item.getQuantity());
            itemDto.setTotalPrice(item.getTotalPrice());
            dto.getItems().add(itemDto);
        }
        return dto;
    }

    @Transactional
    public CartDto addItemToCart(AddCartItemRequest req, Long userId) {

        FoodItemDto food = restaurantClient.getFoodItemByInternal(req.getFoodItemId(), internalApiKey);

        Cart cart = cartRepository.findByCustomerIdAndRestaurantId(userId, req.getRestaurantId());
        if (cart == null) {
            cart = new Cart();
            cart.setCustomerId(userId);
            cart.setRestaurantId(req.getRestaurantId());

            // When we first create a cart, we grab the restaurant name so we can keep it
            // locally.
            try {
                Map<String, Object> restaurant = restaurantClient.getRestaurantById(req.getRestaurantId());
                if (restaurant != null && restaurant.get("name") != null) {
                    cart.setRestaurantName(restaurant.get("name").toString());
                }
            } catch (Exception e) {
                cart.setRestaurantName("Restaurant " + req.getRestaurantId());
            }
            cart = cartRepository.save(cart);
        }

        if (food == null || !food.isAvailable()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Food Item Unavailable");
        }

        if (!food.isRestaurantOpen()) {
            throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "Restaurant is currently closed!");
        }

        // Check if the user already has this item in their cart
        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(i -> i.getFoodItemID().equals(req.getFoodItemId()))
                .findFirst();

        if (existingItem.isPresent()) {
            // If they have it, we just increase the count
            CartItem ci = existingItem.get();
            ci.setQuantity(ci.getQuantity() + req.getQuantity());
            BigDecimal additionalPrice = food.getPrice().multiply(BigDecimal.valueOf(req.getQuantity()));
            ci.setTotalPrice(ci.getTotalPrice().add(additionalPrice));
            cart.setTotalAmount(cart.getTotalAmount().add(additionalPrice));
        } else {
            // If it's a new item, we add it as a fresh row
            CartItem item = new CartItem();
            item.setFoodItemID(req.getFoodItemId());
            item.setFoodName(food.getName());
            item.setQuantity(req.getQuantity());
            item.setTotalPrice(food.getPrice().multiply(BigDecimal.valueOf(req.getQuantity())));
            item.setCart(cart);
            cart.getItems().add(item);
            cart.setTotalAmount(cart.getTotalAmount().add(item.getTotalPrice()));
        }

        return mapToDto(cartRepository.save(cart));
    }

    @Transactional
    public List<CartDto> getAllUserCarts(Long userId) {
        List<Cart> carts = cartRepository.findAllByCustomerId(userId);

        // If some old data is missing names, we fill them in now once and save them.
        boolean needsSave = false;
        for (Cart cart : carts) {
            if (cart.getRestaurantName() == null) {
                try {
                    Map<String, Object> res = restaurantClient.getRestaurantById(cart.getRestaurantId());
                    if (res != null && res.get("name") != null) {
                        cart.setRestaurantName(res.get("name").toString());
                        needsSave = true;
                    }
                } catch (Exception ignored) {
                }
            }
            for (CartItem item : cart.getItems()) {
                if (item.getFoodName() == null) {
                    try {
                        FoodItemDto f = restaurantClient.getFoodItemByInternal(item.getFoodItemID(), internalApiKey);
                        if (f != null && f.getName() != null) {
                            item.setFoodName(f.getName());
                            needsSave = true;
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        }

        if (needsSave) {
            cartRepository.saveAll(carts);
        }

        return carts.stream()
                .map(this::mapToDto)
                .toList();
    }

    @Transactional
    public void clearCart(Long cartId) {
        cartRepository.deleteById(cartId);
    }

    @Transactional
    public void clearAllCarts(Long userId) {
        List<Cart> carts = cartRepository.findAllByCustomerId(userId);
        cartRepository.deleteAll(carts);
    }

    @Transactional
    public CartDto updateCartItemQuantity(Long itemId, int quantity, Long userId) {
        // Find the user's cart containing this item
        List<Cart> userCarts = cartRepository.findAllByCustomerId(userId);

        Cart targetCart = userCarts.stream()
                .filter(cart -> cart.getItems().stream().anyMatch(item -> item.getId().equals(itemId)))
                .findFirst()
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found in any of your carts"));

        CartItem targetItem = targetCart.getItems().stream()
                .filter(item -> item.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart Item not found"));

        FoodItemDto food = restaurantClient.getFoodItemByInternal(targetItem.getFoodItemID(), internalApiKey);

        if (quantity <= 0) {
            // Remove the item if quantity is zero or less
            targetCart.getItems().remove(targetItem);
            targetItem.setCart(null);
        } else {
            // Otherwise, update the quantity and recalculated the price
            targetItem.setQuantity(quantity);
            targetItem.setTotalPrice(food.getPrice().multiply(BigDecimal.valueOf(quantity)));
        }

        // Re-calculate the whole cart's total price
        BigDecimal newTotal = targetCart.getItems().stream()
                .map(CartItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        targetCart.setTotalAmount(newTotal);

        if (targetCart.getItems().isEmpty()) {
            cartRepository.delete(targetCart);
            return null;
        }

        return mapToDto(cartRepository.save(targetCart));
    }

    // These methods update our local database when Kafka tells us a name has
    // changed elsewhere.
    @Transactional
    public void updateRestaurantName(Long restaurantId, String newName) {
        List<Cart> carts = cartRepository.findAllByRestaurantId(restaurantId);
        if (!carts.isEmpty()) {
            carts.forEach(cart -> cart.setRestaurantName(newName));
            cartRepository.saveAll(carts);
        }
    }

    @Transactional
    public void updateFoodName(Long foodItemId, String newName) {
        List<CartItem> items = cartItemRepository.findAllByFoodItemID(foodItemId);
        if (!items.isEmpty()) {
            items.forEach(item -> item.setFoodName(newName));
            cartItemRepository.saveAll(items);
        }
    }
}
