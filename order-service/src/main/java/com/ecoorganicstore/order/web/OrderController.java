package com.ecoorganicstore.order.web;

import com.ecoorganicstore.common.security.AuthGuards;
import com.ecoorganicstore.common.security.UserContextResolver;
import com.ecoorganicstore.common.web.PageResponse;
import com.ecoorganicstore.order.domain.Order;
import com.ecoorganicstore.order.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
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
    public PageResponse<OrderSummaryResponse> orders(HttpServletRequest request,
                                                     @RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "10") int size) {
        String userId = AuthGuards.requireUser(request).userId();
        return map(orderService.ordersByUser(userId, page, size), OrderController::toSummary);
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

    @GetMapping("/api/admin/orders/summary")
    public OrderDeskResponse adminSummary(HttpServletRequest request) {
        AuthGuards.requireAdmin(request);
        OrderService.OrderDesk desk = orderService.desk(Instant.now());
        return new OrderDeskResponse(
                desk.todayCount(),
                desk.todayTotalPaise(),
                desk.confirmedCount(),
                desk.confirmed().stream().map(OrderController::toAdmin).toList(),
                desk.today().stream().map(OrderController::toAdmin).toList());
    }

    @GetMapping("/api/admin/orders")
    public PageResponse<AdminOrderResponse> adminOrders(HttpServletRequest request,
                                                        @RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "20") int size) {
        AuthGuards.requireAdmin(request);
        return map(orderService.allOrders(page, size), OrderController::toAdmin);
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

    private static <T> PageResponse<T> map(Page<Order> page, java.util.function.Function<Order, T> mapper) {
        return PageResponse.of(page.getContent().stream().map(mapper).toList(), page.getNumber(), page.getSize(), page.getTotalElements());
    }

    private static List<LineResponse> linesOf(Order order) {
        if (order.getLines() == null) return List.of();
        return order.getLines().stream()
                .map(line -> new LineResponse(line.productId(), line.productName(), line.pricePaise(), line.qty(), line.image()))
                .toList();
    }

    public record OrderDeskResponse(
            long todayCount,
            long todayTotalPaise,
            long confirmedCount,
            List<AdminOrderResponse> confirmed,
            List<AdminOrderResponse> today) {}
    public record CheckoutRequest(String shippingAddress) {}
    public record PurchaseResponse(boolean purchased) {}
    public record StatusRequest(@NotBlank(message = "Choose the next order status.") String status) {}
    public record LineResponse(String productId, String productName, long pricePaise, int qty, String image) {}
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