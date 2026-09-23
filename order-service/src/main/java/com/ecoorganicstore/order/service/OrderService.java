package com.ecoorganicstore.order.service;

import com.ecoorganicstore.order.domain.Fulfillment;
import com.ecoorganicstore.order.domain.Order;
import com.ecoorganicstore.order.repo.OrderRepository;
import com.ecoorganicstore.common.web.ForbiddenException;
import com.ecoorganicstore.common.web.UnauthorizedException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final RestClient restClient;
    private final String internalKey;
    private final OrderNotifier orderNotifier;

    @Value("${services.cart:http://localhost:8083}")
    private String cartUrl;
    @Value("${services.inventory:http://localhost:8084}")
    private String inventoryUrl;
    @Value("${services.catalog:http://localhost:8082}")
    private String catalogUrl;
    @Value("${services.payment:http://localhost:8086}")
    private String paymentUrl;

    public OrderService(OrderRepository orderRepository, RestClient restClient,
                        @Value("${app.internal-key}") String internalKey, OrderNotifier orderNotifier) {
        this.orderRepository = orderRepository;
        this.restClient = restClient;
        this.internalKey = internalKey;
        this.orderNotifier = orderNotifier;
    }

    public CheckoutResponse checkout(String userId, String shippingAddress) {
        if (userId == null || userId.isBlank()) throw new UnauthorizedException("Authentication required");
        Map cart = restClient.get().uri(cartUrl + "/internal/cart/" + userId).header("X-Internal-Key", internalKey).retrieve().body(Map.class);
        List<Map<String, Object>> items = (List<Map<String, Object>>) cart.get("items");
        if (items == null || items.isEmpty()) throw new IllegalArgumentException("Cart is empty");

        List<Map<String, Object>> reserveLines = new ArrayList<>();
        List<Order.Line> lines = new ArrayList<>();
        long total = 0;
        for (Map<String, Object> item : items) {
            String productId = String.valueOf(item.get("productId"));
            int qty = ((Number) item.get("qty")).intValue();
            Map product = restClient.get().uri(catalogUrl + "/internal/products/" + productId).header("X-Internal-Key", internalKey).retrieve().body(Map.class);
            long price = ((Number) product.get("pricePaise")).longValue();
            String name = String.valueOf(product.get("name"));
            total += price * qty;
            lines.add(new Order.Line(productId, name, price, qty));
            reserveLines.add(Map.of("productId", productId, "qty", qty));
        }

        String orderNumber = "HC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Map reserve = restClient.post().uri(inventoryUrl + "/internal/inventory/reserve")
                .header("X-Internal-Key", internalKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("orderId", orderNumber, "lines", reserveLines))
                .retrieve().body(Map.class);

        Order order = new Order();
        order.setOrderNumber(orderNumber);
        order.setUserId(userId);
        order.setShippingAddress(shippingAddress);
        order.setLines(lines);
        order.setTotalPaise(total);
        order.setOrderStatus("PENDING_PAYMENT");
        order.setReservationId(String.valueOf(reserve.get("id")));
        order = orderRepository.save(order);

        Map session = restClient.post().uri(paymentUrl + "/internal/payments/session")
                .header("X-Internal-Key", internalKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("orderNumber", orderNumber, "amountPaise", total, "lineItems", lines))
                .retrieve().body(Map.class);

        order.setPaymentId(String.valueOf(session.get("paymentId")));
        orderRepository.save(order);
        return new CheckoutResponse(orderNumber, String.valueOf(session.get("checkoutUrl")));
    }

    public List<Order> ordersByUser(String userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<Order> allOrders() {
        return orderRepository.findAll().stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .toList();
    }

    public Order orderByNumber(String userId, String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber).orElseThrow(() -> new IllegalArgumentException("Order not found"));
        if (!order.getUserId().equals(userId)) throw new ForbiddenException("You cannot access this order");
        return order;
    }

    public Order markPaid(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber).orElseThrow(() -> new IllegalArgumentException("Order not found"));
        order.setOrderStatus("CONFIRMED");
        orderRepository.save(order);
        restClient.post().uri(inventoryUrl + "/internal/inventory/confirm/" + orderNumber)
                .header("X-Internal-Key", internalKey).retrieve().toBodilessEntity();
        restClient.delete().uri(cartUrl + "/internal/cart/" + order.getUserId())
                .header("X-Internal-Key", internalKey).retrieve().toBodilessEntity();
        orderNotifier.statusChanged(order);
        return order;
    }

    public boolean userPurchasedProduct(String userId, String productId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .filter(o -> "DELIVERED".equals(o.getOrderStatus()) || "CONFIRMED".equals(o.getOrderStatus()))
                .flatMap(o -> o.getLines().stream())
                .anyMatch(l -> l.productId().equals(productId));
    }

    public Order updateStatus(String orderNumber, String status) {
        Order order = orderRepository.findByOrderNumber(orderNumber).orElseThrow(() -> new IllegalArgumentException("Order not found"));
        order.setOrderStatus(Fulfillment.advance(order.getOrderStatus(), status));
        Order saved = orderRepository.save(order);
        orderNotifier.statusChanged(saved);
        return saved;
    }

    public record CheckoutResponse(String orderNumber, String checkoutUrl) {}
}