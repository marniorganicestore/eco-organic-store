package com.ecoorganicstore.cart.service;

import java.util.Collection;
import java.util.Map;

public interface StockLookup {
    StockSnapshot availableFor(Collection<String> productIds);
}

record StockSnapshot(boolean known, Map<String, Integer> available) {
    StockSnapshot {
        available = available == null ? Map.of() : Map.copyOf(available);
    }

    static StockSnapshot unknown() {
        return new StockSnapshot(false, Map.of());
    }

    static StockSnapshot of(Map<String, Integer> available) {
        return new StockSnapshot(true, available);
    }
}
