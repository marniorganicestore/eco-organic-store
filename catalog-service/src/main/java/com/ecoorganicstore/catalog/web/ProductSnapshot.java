package com.ecoorganicstore.catalog.web;

import com.ecoorganicstore.catalog.domain.Product;

public record ProductSnapshot(
        String id,
        String slug,
        String name,
        long pricePaise,
        String image,
        String unit,
        String origin,
        boolean active
) {
    public static ProductSnapshot from(Product product) {
        String image = product.getImages() == null || product.getImages().isEmpty() ? null : product.getImages().getFirst();
        return new ProductSnapshot(
                product.getId(),
                product.getSlug(),
                product.getName(),
                product.getPricePaise(),
                image,
                product.getUnit(),
                product.getOrigin(),
                product.isActive()
        );
    }
}
