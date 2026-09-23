package com.ecoorganicstore.order.domain;

import java.util.Locale;
import java.util.Set;

/**
 * Paid orders move one step at a time: confirmed, packed, shipped, delivered.
 * Payment confirmation stays with the checkout saga, so the desk cannot mark an order paid.
 */
public final class Fulfillment {
    private Fulfillment() {}

    public static String advance(String current, String requested) {
        String from = current == null ? "" : current.trim().toUpperCase(Locale.ROOT);
        if (requested == null || requested.isBlank()) {
            throw new IllegalArgumentException("Choose the next order status.");
        }
        String to = requested.trim().toUpperCase(Locale.ROOT);
        if (!allowed(from).contains(to)) {
            throw new IllegalArgumentException(rejection(from));
        }
        return to;
    }

    private static Set<String> allowed(String from) {
        return switch (from) {
            case "CONFIRMED" -> Set.of("PACKED");
            case "PACKED" -> Set.of("SHIPPED");
            case "SHIPPED" -> Set.of("DELIVERED");
            default -> Set.of();
        };
    }

    private static String rejection(String from) {
        return switch (from) {
            case "PENDING_PAYMENT" -> "This order is still waiting for payment.";
            case "DELIVERED" -> "Delivered orders stay delivered.";
            case "CANCELLED" -> "Cancelled orders cannot be fulfilled.";
            case "CONFIRMED" -> "Pack this order before it can ship.";
            case "PACKED" -> "Ship this order before it can be delivered.";
            case "SHIPPED" -> "A shipped order can only be marked delivered.";
            default -> "This order cannot change status from here.";
        };
    }
}
