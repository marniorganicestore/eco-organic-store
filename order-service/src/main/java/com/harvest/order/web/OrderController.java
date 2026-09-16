package com.harvest.order.web;

import com.harvest.common.security.AuthGuards;
import com.harvest.common.security.UserContextResolver;
import com.harvest.order.domain.Order;
import com.harvest.order.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/api/checkout/sessions")
    public OrderService.CheckoutResponse checkout(HttpServletRequest request, @RequestBody CheckoutRequest checkoutRequest) {
        String userId = UserContextResolver.fromHeaders(request).userId();
        return orderService.checkout(userId, checkoutRequest.shippingAddress());
    }

    @GetMapping("/api/orders")
    public List<Order> orders(HttpServletRequest request) {
        String userId = AuthGuards.requireUser(request).userId();
        return orderService.ordersByUser(userId);
    }

    @GetMapping("/api/orders/{orderNumber}")
    public Order order(HttpServletRequest request, @PathVariable String orderNumber) {
        String userId = AuthGuards.requireUser(request).userId();
        return orderService.orderByNumber(userId, orderNumber);
    }

    @PostMapping("/internal/orders/{orderNumber}/paid")
    public Order markPaid(@PathVariable String orderNumber) {
        return orderService.markPaid(orderNumber);
    }

    @GetMapping("/internal/orders/{userId}/purchased/{productId}")
    public PurchaseResponse purchased(@PathVariable String userId, @PathVariable String productId) {
        return new PurchaseResponse(orderService.userPurchasedProduct(userId, productId));
    }

    @GetMapping("/api/admin/orders")
    public List<Order> adminOrders(HttpServletRequest request) {
        AuthGuards.requireAdmin(request);
        return orderService.allOrders();
    }

    @PatchMapping("/api/admin/orders/{orderNumber}")
    public Order adminUpdate(HttpServletRequest request, @PathVariable String orderNumber, @RequestBody StatusRequest statusRequest) {
        AuthGuards.requireAdmin(request);
        return orderService.updateStatus(orderNumber, statusRequest.status());
    }

    public record CheckoutRequest(String shippingAddress) {}
    public record PurchaseResponse(boolean purchased) {}
    public record StatusRequest(String status) {}
}