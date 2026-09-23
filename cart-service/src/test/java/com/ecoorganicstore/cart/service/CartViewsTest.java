package com.ecoorganicstore.cart.service;

import com.ecoorganicstore.cart.domain.Cart;
import com.ecoorganicstore.cart.web.CartResponse;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CartViewsTest {
    @Test
    void pricesActiveLinesAndKeepsBasketOrder() {
        CatalogProduct spinach = product("spinach", "Baby Spinach", 17900, true);
        CatalogProduct tomatoes = product("tomatoes", "Cherry Tomatoes", 24900, true);

        CartResponse cart = CartViews.compose(
                List.of(new Cart.Item("spinach", 2), new Cart.Item("tomatoes", 1)),
                Map.of("spinach", spinach, "tomatoes", tomatoes),
                StockSnapshot.of(Map.of("spinach", 8, "tomatoes", 4))
        );

        assertEquals(List.of("Baby Spinach", "Cherry Tomatoes"), cart.items().stream().map(CartResponse.Line::name).toList());
        assertEquals(3, cart.itemCount());
        assertEquals(17900L * 2 + 24900L, cart.subtotalPaise());
        assertTrue(cart.items().getFirst().purchasable());
        assertEquals(8, cart.items().getFirst().available());
    }

    @Test
    void holdsCheckoutWhenALineExceedsStockOrIsGone() {
        CatalogProduct spinach = product("spinach", "Baby Spinach", 17900, true);

        CartResponse cart = CartViews.compose(
                List.of(new Cart.Item("spinach", 4), new Cart.Item("missing", 1)),
                Map.of("spinach", spinach),
                StockSnapshot.of(Map.of("spinach", 2))
        );

        assertFalse(cart.items().get(0).purchasable());
        assertEquals(2, cart.items().get(0).available());
        assertEquals("No longer available", cart.items().get(1).name());
        assertFalse(cart.items().get(1).purchasable());
        assertEquals(0, cart.subtotalPaise());
        assertEquals(5, cart.itemCount());
    }

    @Test
    void keepsALinePurchasableWhenStockCannotBeRead() {
        CatalogProduct spinach = product("spinach", "Baby Spinach", 17900, true);

        CartResponse cart = CartViews.compose(
                List.of(new Cart.Item("spinach", 1)),
                Map.of("spinach", spinach),
                StockSnapshot.unknown()
        );

        assertTrue(cart.items().getFirst().purchasable());
        assertNull(cart.items().getFirst().available());
        assertEquals(17900L, cart.subtotalPaise());
    }

    private static CatalogProduct product(String id, String name, long pricePaise, boolean active) {
        return new CatalogProduct(id, id, name, pricePaise, "/images/" + id + ".jpg", "250 g", "Nilgiris", active);
    }
}
