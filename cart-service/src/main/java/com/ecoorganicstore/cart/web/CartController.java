package com.ecoorganicstore.cart.web;

import com.ecoorganicstore.cart.service.CartService;
import com.ecoorganicstore.common.security.AuthGuards;
import com.ecoorganicstore.common.security.UserContextResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class CartController {
    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping("/api/cart")
    public CartResponse cart(HttpServletRequest request, @RequestParam(required = false) String guestToken) {
        String userId = UserContextResolver.fromHeaders(request).userId();
        return cartService.view(userId, guestToken);
    }

    @PostMapping("/api/cart")
    public CartResponse add(HttpServletRequest request, @Valid @RequestBody ItemRequest itemRequest,
                            @RequestParam(required = false) String guestToken) {
        String userId = UserContextResolver.fromHeaders(request).userId();
        return cartService.addItem(userId, guestToken, itemRequest.productId(), itemRequest.qty());
    }

    @PatchMapping("/api/cart")
    public CartResponse update(HttpServletRequest request, @Valid @RequestBody ItemRequest itemRequest,
                               @RequestParam(required = false) String guestToken) {
        String userId = UserContextResolver.fromHeaders(request).userId();
        return cartService.updateQty(userId, guestToken, itemRequest.productId(), itemRequest.qty());
    }

    @DeleteMapping("/api/cart")
    public void clear(HttpServletRequest request) {
        String userId = AuthGuards.requireUser(request).userId();
        cartService.clear(userId);
    }

    @PostMapping("/api/cart/merge")
    public CartResponse merge(HttpServletRequest request, @RequestParam String guestToken) {
        String userId = AuthGuards.requireUser(request).userId();
        return cartService.merge(userId, guestToken);
    }

    @GetMapping("/internal/cart/{userId}")
    public InternalCartResponse internalCart(@PathVariable String userId) {
        return cartService.internalLines(userId);
    }

    @DeleteMapping("/internal/cart/{userId}")
    public void internalClear(@PathVariable String userId) {
        cartService.clear(userId);
    }

    public record ItemRequest(
            @NotBlank(message = "Choose a product.") String productId,
            @Min(value = 0, message = "Quantity cannot be negative.") int qty
    ) {}
}
