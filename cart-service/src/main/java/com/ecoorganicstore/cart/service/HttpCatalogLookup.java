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
public class HttpCatalogLookup implements CatalogLookup {
    private final RestClient restClient;
    private final String internalKey;
    private final String catalogUrl;

    public HttpCatalogLookup(RestClient restClient,
                             @Value("${app.internal-key}") String internalKey,
                             @Value("${services.catalog:http://localhost:8082}") String catalogUrl) {
        this.restClient = restClient;
        this.internalKey = internalKey;
        this.catalogUrl = catalogUrl;
    }

    @Override
    public CatalogProduct requirePurchasable(String productId) {
        if (productId == null || productId.isBlank()) {
            throw new IllegalArgumentException("Choose a product.");
        }
        CatalogProduct product;
        try {
            product = findByIds(List.of(productId)).get(productId);
        } catch (RestClientException ex) {
            throw new IllegalArgumentException("We could not confirm this product. Try again.");
        }
        if (product == null || !product.active()) {
            throw new IllegalArgumentException("This product is no longer available.");
        }
        return product;
    }

    @Override
    public Map<String, CatalogProduct> findByIds(Collection<String> productIds) {
        List<String> ids = distinctIds(productIds);
        if (ids.isEmpty()) return Map.of();
        String query = ids.stream()
                .map(id -> "ids=" + URLEncoder.encode(id, StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
        CatalogProduct[] body = restClient.get()
                .uri(catalogUrl + "/internal/products?" + query)
                .header("X-Internal-Key", internalKey)
                .retrieve()
                .body(CatalogProduct[].class);
        if (body == null) return Map.of();
        Map<String, CatalogProduct> products = new HashMap<>();
        for (CatalogProduct product : body) {
            if (product != null && product.id() != null) products.put(product.id(), product);
        }
        return products;
    }

    static List<String> distinctIds(Collection<String> productIds) {
        if (productIds == null || productIds.isEmpty()) return List.of();
        return productIds.stream().filter(id -> id != null && !id.isBlank()).distinct().toList();
    }
}
