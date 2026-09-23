package com.ecoorganicstore.cart.web;

import java.util.List;

public record CartResponse(List<Line> items, int itemCount, long subtotalPaise) {
    public CartResponse {
        items = items == null ? List.of() : List.copyOf(items);
    }

    public static CartResponse empty() {
        return new CartResponse(List.of(), 0, 0);
    }

    public record Line(
            String productId,
            int qty,
            String slug,
            String name,
            String unit,
            String origin,
            String image,
            long pricePaise,
            long lineTotalPaise,
            Integer available,
            boolean purchasable
    ) {}
}
