package com.ecoorganicstore.cart.service;

import com.ecoorganicstore.cart.domain.Cart;
import com.ecoorganicstore.cart.web.CartResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class CartViews {
    private CartViews() {}

    static CartResponse compose(List<Cart.Item> items, Map<String, CatalogProduct> products, StockSnapshot stock) {
        if (items == null || items.isEmpty()) return CartResponse.empty();
        List<CartResponse.Line> lines = new ArrayList<>();
        long subtotal = 0;
        int itemCount = 0;
        for (Cart.Item item : items) {
            if (item == null || item.productId() == null || item.productId().isBlank() || item.qty() <= 0) continue;
            CatalogProduct product = products == null ? null : products.get(item.productId());
            boolean known = product != null;
            boolean active = known && product.active();
            Integer available = stock != null && stock.known()
                    ? stock.available().getOrDefault(item.productId(), 0)
                    : null;
            boolean inStock = available == null || item.qty() <= available;
            boolean purchasable = active && inStock;
            long price = known ? product.pricePaise() : 0;
            long lineTotal = known ? Math.multiplyExact(price, item.qty()) : 0;
            itemCount += item.qty();
            if (purchasable) subtotal += lineTotal;
            lines.add(new CartResponse.Line(
                    item.productId(),
                    item.qty(),
                    known ? emptyIfNull(product.slug()) : "",
                    known ? emptyIfNull(product.name()) : "No longer available",
                    known ? emptyIfNull(product.unit()) : "",
                    known ? emptyIfNull(product.origin()) : "",
                    known ? product.image() : null,
                    price,
                    lineTotal,
                    available,
                    purchasable
            ));
        }
        return new CartResponse(lines, itemCount, subtotal);
    }

    private static String emptyIfNull(String value) {
        return value == null ? "" : value;
    }
}
