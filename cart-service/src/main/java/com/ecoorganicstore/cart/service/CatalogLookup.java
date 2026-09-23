package com.ecoorganicstore.cart.service;

import java.util.Collection;
import java.util.Map;

public interface CatalogLookup {
    CatalogProduct requirePurchasable(String productId);

    Map<String, CatalogProduct> findByIds(Collection<String> productIds);
}
