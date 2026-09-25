package com.ecoorganicstore.order.web;

import com.ecoorganicstore.common.security.AuthGuards;
import com.ecoorganicstore.common.security.UserContextResolver;
import com.ecoorganicstore.order.domain.Order;
import com.ecoorganicstore.order.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
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
    public List<OrderSummaryResponse> orders(HttpServletRequest request) {
        String userId = AuthGuards.requireUser(request).userId();
        return orderService.ordersByUser(userId).stream().map(OrderController::toSummary).toList();
    }

    @GetMapping("/api/orders/{orderNumber}")
    public OrderSummaryResponse order(HttpServletRequest request, @PathVariable String orderNumber) {
        String userId = AuthGuards.requireUser(request).userId();
        return toSummary(orderService.orderByNumber(userId, orderNumber));
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
    public List<AdminOrderResponse> adminOrders(HttpServletRequest request) {
        AuthGuards.requireAdmin(request);
        return orderService.allOrders().stream().map(OrderController::toAdmin).toList();
    }

    @PatchMapping("/api/admin/orders/{orderNumber}")
    public AdminOrderResponse adminUpdate(HttpServletRequest request, @PathVariable String orderNumber,
                                          @Valid @RequestBody StatusRequest statusRequest) {
        AuthGuards.requireAdmin(request);
        return toAdmin(orderService.updateStatus(orderNumber, statusRequest.status()));
    }

    private static OrderSummaryResponse toSummary(Order order) {
        return new OrderSummaryResponse(
                order.getId(),
                order.getOrderNumber(),
                linesOf(order),
                order.getShippingAddress(),
                order.getTotalPaise(),
                order.getOrderStatus(),
                order.getCreatedAt());
    }

    private static AdminOrderResponse toAdmin(Order order) {
        return new AdminOrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getUserId(),
                linesOf(order),
                order.getShippingAddress(),
                order.getTotalPaise(),
                order.getOrderStatus(),
                order.getCreatedAt());
    }

    private static List<LineResponse> linesOf(Order order) {
        if (order.getLines() == null) return List.of();
        return order.getLines().stream()
                .map(line -> new LineResponse(line.productId(), line.productName(), line.pricePaise(), line.qty()))
                .toList();
    }

    public record CheckoutRequest(String shippingAddress) {}
    public record PurchaseResponse(boolean purchased) {}
    public record StatusRequest(@NotBlank(message = "Choose the next order status.") String status) {}
    public record LineResponse(String productId, String productName, long pricePaise, int qty) {}
    public record OrderSummaryResponse(
            String id,
            String orderNumber,
            List<LineResponse> lines,
            String shippingAddress,
            long totalPaise,
            String orderStatus,
            Instant createdAt) {}
    public record AdminOrderResponse(
            String id,
            String orderNumber,
            String userId,
            List<LineResponse> lines,
            String shippingAddress,
            long totalPaise,
            String orderStatus,
            Instant createdAt) {}
}