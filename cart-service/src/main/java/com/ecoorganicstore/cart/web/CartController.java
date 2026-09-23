package com.ecoorganicstore.cart.web;

import com.ecoorganicstore.cart.domain.Cart;
import com.ecoorganicstore.cart.service.CartService;
import com.ecoorganicstore.common.security.AuthGuards;
import com.ecoorganicstore.common.security.UserContextResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping
public class CartController {
    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping("/api/cart")
    public Cart cart(HttpServletRequest request, @RequestParam(required = false) String guestToken) {
        String userId = UserContextResolver.fromHeaders(request).userId();
        return cartService.getCart(userId, guestToken);
    }

    @PostMapping("/api/cart")
    public Cart add(HttpServletRequest request, @RequestBody ItemRequest itemRequest, @RequestParam(required = false) String guestToken) {
        String userId = UserContextResolver.fromHeaders(request).userId();
        return cartService.addItem(userId, guestToken, itemRequest.productId(), itemRequest.qty());
    }

    @PatchMapping("/api/cart")
    public Cart update(HttpServletRequest request, @RequestBody ItemRequest itemRequest, @RequestParam(required = false) String guestToken) {
        String userId = UserContextResolver.fromHeaders(request).userId();
        return cartService.updateQty(userId, guestToken, itemRequest.productId(), itemRequest.qty());
    }

    @DeleteMapping("/api/cart")
    public void clear(HttpServletRequest request) {
        String userId = AuthGuards.requireUser(request).userId();
        cartService.clear(userId);
    }

    @PostMapping("/api/cart/merge")
    public Cart merge(HttpServletRequest request, @RequestParam String guestToken) {
        String userId = AuthGuards.requireUser(request).userId();
        return cartService.merge(userId, guestToken);
    }

    @GetMapping("/internal/cart/{userId}")
    public Cart internalCart(@PathVariable String userId) {
        return cartService.getCart(userId, null);
    }

    @DeleteMapping("/internal/cart/{userId}")
    public void internalClear(@PathVariable String userId) {
        cartService.clear(userId);
    }

    public record ItemRequest(String productId, int qty) {}
}