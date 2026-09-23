package com.ecoorganicstore.cart.web;

import java.util.List;

public record InternalCartResponse(List<Line> items) {
    public InternalCartResponse {
        items = items == null ? List.of() : List.copyOf(items);
    }

    public record Line(String productId, int qty) {}
}
