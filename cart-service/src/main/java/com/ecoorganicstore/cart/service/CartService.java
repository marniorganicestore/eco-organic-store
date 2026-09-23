package com.ecoorganicstore.cart.service;

import com.ecoorganicstore.cart.domain.Cart;
import com.ecoorganicstore.cart.repo.CartRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class CartService {
    private final CartRepository cartRepository;
    private final RestClient restClient;
    private final String internalKey;
    private final String catalogUrl;
    private final String inventoryUrl;

    public CartService(CartRepository cartRepository,
                       RestClient restClient,
                       @Value("${app.internal-key}") String internalKey,
                       @Value("${services.catalog:http://localhost:8082}") String catalogUrl,
                       @Value("${services.inventory:http://localhost:8084}") String inventoryUrl) {
        this.cartRepository = cartRepository;
        this.restClient = restClient;
        this.internalKey = internalKey;
        this.catalogUrl = catalogUrl;
        this.inventoryUrl = inventoryUrl;
    }

    public Cart getCart(String userId, String guestToken) {
        if (userId != null && !userId.isBlank()) {
            return cartRepository.findByUserId(userId).orElseGet(() -> createUserCart(userId));
        }
        if (guestToken == null || guestToken.isBlank()) {
            throw new IllegalArgumentException("Guest token is required for guest cart");
        }
        return cartRepository.findByGuestToken(guestToken).orElseGet(() -> createGuestCart(guestToken));
    }

    public Cart addItem(String userId, String guestToken, String productId, int qty) {
        validateProductAndStock(productId, qty);
        Cart cart = getCart(userId, guestToken);
        Map<String, Integer> map = new HashMap<>();
        for (var item : cart.getItems()) map.put(item.productId(), item.qty());
        map.put(productId, map.getOrDefault(productId, 0) + qty);
        cart.setItems(toItems(map));
        cart.setUpdatedAt(Instant.now());
        return cartRepository.save(cart);
    }

    public Cart updateQty(String userId, String guestToken, String productId, int qty) {
        if (qty > 0) {
            validateProductAndStock(productId, qty);
        }
        Cart cart = getCart(userId, guestToken);
        Map<String, Integer> map = new HashMap<>();
        for (var item : cart.getItems()) map.put(item.productId(), item.qty());
        if (qty <= 0) map.remove(productId); else map.put(productId, qty);
        cart.setItems(toItems(map));
        cart.setUpdatedAt(Instant.now());
        return cartRepository.save(cart);
    }

    public Cart merge(String userId, String guestToken) {
        if (userId == null || userId.isBlank() || guestToken == null || guestToken.isBlank()) {
            throw new IllegalArgumentException("userId and guestToken required");
        }
        Cart user = cartRepository.findByUserId(userId).orElseGet(() -> createUserCart(userId));
        Cart guest = cartRepository.findByGuestToken(guestToken).orElseGet(() -> createGuestCart(guestToken));
        Map<String, Integer> map = new HashMap<>();
        for (var item : user.getItems()) map.put(item.productId(), item.qty());
        for (var item : guest.getItems()) map.put(item.productId(), map.getOrDefault(item.productId(), 0) + item.qty());
        user.setItems(toItems(map));
        user.setUpdatedAt(Instant.now());
        cartRepository.delete(guest);
        return cartRepository.save(user);
    }

    public void clear(String userId) {
        cartRepository.findByUserId(userId).ifPresent(c -> {
            c.setItems(List.of());
            c.setUpdatedAt(Instant.now());
            cartRepository.save(c);
        });
    }

    private Cart createUserCart(String userId) {
        Cart cart = new Cart();
        cart.setUserId(userId);
        return cartRepository.save(cart);
    }

    private Cart createGuestCart(String guestToken) {
        Cart cart = new Cart();
        cart.setGuestToken(guestToken);
        return cartRepository.save(cart);
    }

    private List<Cart.Item> toItems(Map<String, Integer> map) {
        List<Cart.Item> items = new ArrayList<>();
        map.forEach((k,v) -> { if (v > 0) items.add(new Cart.Item(k, v)); });
        return items;
    }

    @SuppressWarnings("unchecked")
    private void validateProductAndStock(String productId, int requestedQty) {
        restClient.get()
                .uri(catalogUrl + "/internal/products/" + productId)
                .header("X-Internal-Key", internalKey)
                .retrieve()
                .body(Map.class);
        List<Map<String, Object>> stocks = restClient.get()
                .uri(inventoryUrl + "/internal/stock?productIds=" + productId)
                .header("X-Internal-Key", internalKey)
                .retrieve()
                .body(List.class);
        if (stocks == null || stocks.isEmpty()) {
            throw new IllegalArgumentException("Stock is unavailable for this product");
        }
        int available = ((Number) stocks.get(0).get("available")).intValue();
        if (available < requestedQty) {
            throw new IllegalArgumentException("Requested quantity is not available");
        }
    }
}