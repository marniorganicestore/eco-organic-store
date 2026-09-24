package com.ecoorganicstore.order.service;

import com.ecoorganicstore.common.web.ForbiddenException;
import com.ecoorganicstore.common.web.UnauthorizedException;
import com.ecoorganicstore.order.domain.Fulfillment;
import com.ecoorganicstore.order.domain.Order;
import com.ecoorganicstore.order.repo.OrderRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Service
public class OrderService {
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

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

        PaymentSession session;
        try {
            session = restClient.post().uri(paymentUrl + "/internal/payments/session")
                    .header("X-Internal-Key", internalKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("orderNumber", orderNumber, "amountPaise", total))
                    .retrieve().body(PaymentSession.class);
        } catch (RestClientResponseException ex) {
            log.warn("Payment session failed for {}", orderNumber, ex);
            abandonUnpaidCheckout(order);
            String message = paymentFailureMessage(ex);
            int status = ex.getStatusCode().value();
            if (status == 401 || status == 403) {
                throw new UnauthorizedException(message);
            }
            throw new IllegalArgumentException(message);
        } catch (RuntimeException ex) {
            log.warn("Payment session failed for {}", orderNumber, ex);
            abandonUnpaidCheckout(order);
            throw new IllegalArgumentException(paymentFailureMessage(ex));
        }
        if (session == null || session.paymentId() == null || session.paymentId().isBlank()) {
            abandonUnpaidCheckout(order);
            throw new IllegalArgumentException("Payment could not be started. Nothing was charged. Please try again.");
        }
        String razorpayOrderId = blankToEmpty(session.orderId());
        String keyId = blankToEmpty(session.keyId());
        String checkoutUrl = blankToEmpty(session.checkoutUrl());
        boolean standardCheckout = !razorpayOrderId.isBlank() && !keyId.isBlank();
        if (!standardCheckout && checkoutUrl.isBlank()) {
            abandonUnpaidCheckout(order);
            throw new IllegalArgumentException("Payment could not be started. Nothing was charged. Please try again.");
        }

        order.setPaymentId(session.paymentId());
        orderRepository.save(order);
        long amount = session.amount() > 0 ? session.amount() : total;
        String currency = session.currency() == null || session.currency().isBlank() ? "INR" : session.currency();
        return new CheckoutResponse(orderNumber, razorpayOrderId, amount, currency, keyId, checkoutUrl);
    }

    private static String blankToEmpty(String value) {
        return value == null ? "" : value;
    }

    static String paymentFailureMessage(RuntimeException ex) {
        String detail = "";
        if (ex instanceof RestClientResponseException response) {
            detail = problemDetail(response.getResponseBodyAsString());
        }
        if (detail.isBlank()) {
            return "Payment could not be started. Nothing was charged. Please try again.";
        }
        return detail;
    }

    private static String problemDetail(String body) {
        if (body == null || body.isBlank()) {
            return "";
        }
        try {
            JsonNode detail = new ObjectMapper().readTree(body).path("detail");
            String text = detail.asText("").trim();
            if (text.length() > 240) {
                text = text.substring(0, 240);
            }
            return text;
        } catch (Exception ex) {
            return "";
        }
    }

    private void abandonUnpaidCheckout(Order order) {
        try {
            restClient.post().uri(inventoryUrl + "/internal/inventory/release/" + order.getOrderNumber())
                    .header("X-Internal-Key", internalKey)
                    .retrieve().toBodilessEntity();
        } catch (RuntimeException ex) {
            log.warn("Could not release stock for unpaid order {}", order.getOrderNumber(), ex);
        }
        try {
            order.setOrderStatus("CANCELLED");
            orderRepository.save(order);
        } catch (RuntimeException ex) {
            log.warn("Could not cancel unpaid order {}", order.getOrderNumber(), ex);
        }
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
        if (!"PENDING_PAYMENT".equals(order.getOrderStatus())) {
            return order;
        }
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

    public record PaymentSession(String paymentId, String orderId, long amount, String currency, String keyId, String checkoutUrl) {}

    public record CheckoutResponse(String orderNumber, String orderId, long amount, String currency, String keyId, String checkoutUrl) {}
}