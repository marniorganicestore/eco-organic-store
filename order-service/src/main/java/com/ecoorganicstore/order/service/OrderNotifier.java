package com.ecoorganicstore.order.service;

import com.ecoorganicstore.order.domain.Order;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class OrderNotifier {
    private static final Logger log = LoggerFactory.getLogger(OrderNotifier.class);

    private final RestClient restClient;
    private final String internalKey;
    private final String identityUrl;

    public OrderNotifier(
            RestClient restClient,
            @Value("${app.internal-key}") String internalKey,
            @Value("${services.identity:http://localhost:8081}") String identityUrl) {
        this.restClient = restClient;
        this.internalKey = internalKey;
        this.identityUrl = identityUrl;
    }

    public void statusChanged(Order order) {
        try {
            restClient.post()
                    .uri(identityUrl + "/internal/notifications/orders")
                    .header("X-Internal-Key", internalKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(OrderNotice.from(order))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RuntimeException ex) {
            log.warn("Order email was not queued for {}: {}", order.getOrderNumber(), ex.toString());
        }
    }

    record OrderNotice(
            String userId,
            String orderNumber,
            String orderStatus,
            long totalPaise,
            String shippingAddress,
            List<Line> lines) {
        record Line(String productName, int qty, long pricePaise) {}

        static OrderNotice from(Order order) {
            List<Line> lines = order.getLines() == null
                    ? List.of()
                    : order.getLines().stream()
                            .map(line -> new Line(line.productName(), line.qty(), line.pricePaise()))
                            .toList();
            return new OrderNotice(
                    order.getUserId(),
                    order.getOrderNumber(),
                    order.getOrderStatus(),
                    order.getTotalPaise(),
                    order.getShippingAddress(),
                    lines);
        }
    }
}
