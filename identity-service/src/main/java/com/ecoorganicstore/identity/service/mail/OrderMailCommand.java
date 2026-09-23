package com.ecoorganicstore.identity.service.mail;

import java.util.List;

public record OrderMailCommand(
        String customerName,
        String customerEmail,
        boolean customerOptIn,
        String orderNumber,
        String orderStatus,
        long totalPaise,
        String shippingAddress,
        List<Line> lines) {
    public record Line(String name, int qty, long pricePaise) {}
}
