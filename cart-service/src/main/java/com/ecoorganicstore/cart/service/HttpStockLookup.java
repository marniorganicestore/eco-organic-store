package com.ecoorganicstore.cart.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class HttpStockLookup implements StockLookup {
    private final RestClient restClient;
    private final String internalKey;
    private final String inventoryUrl;

    public HttpStockLookup(RestClient restClient,
                           @Value("${app.internal-key}") String internalKey,
                           @Value("${services.inventory:http://localhost:8084}") String inventoryUrl) {
        this.restClient = restClient;
        this.internalKey = internalKey;
        this.inventoryUrl = inventoryUrl;
    }

    @Override
    public StockSnapshot availableFor(Collection<String> productIds) {
        List<String> ids = HttpCatalogLookup.distinctIds(productIds);
        if (ids.isEmpty()) return StockSnapshot.of(Map.of());
        String query = ids.stream()
                .map(id -> "productIds=" + URLEncoder.encode(id, StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
        try {
            StockLine[] body = restClient.get()
                    .uri(inventoryUrl + "/internal/stock?" + query)
                    .header("X-Internal-Key", internalKey)
                    .retrieve()
                    .body(StockLine[].class);
            Map<String, Integer> available = new HashMap<>();
            if (body != null) {
                for (StockLine line : body) {
                    if (line != null && line.productId() != null) available.put(line.productId(), line.available());
                }
            }
            return StockSnapshot.of(available);
        } catch (RestClientException ex) {
            return StockSnapshot.unknown();
        }
    }

    record StockLine(String productId, int available) {}
}
