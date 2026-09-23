package com.ecoorganicstore.cart.service;

public record CatalogProduct(
        String id,
        String slug,
        String name,
        long pricePaise,
        String image,
        String unit,
        String origin,
        boolean active
) {}
