package com.ecoorganicstore.cart.service;

import com.ecoorganicstore.cart.domain.Cart;
import com.ecoorganicstore.cart.repo.CartRepository;
import com.ecoorganicstore.cart.web.CartResponse;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CartServiceTest {
    private final AtomicReference<Cart> stored = new AtomicReference<>();
    private CartRepository repository;
    private CatalogLookup catalog;
    private StockLookup stock;
    private CartService service;

    @BeforeEach
    void setUp() {
        stored.set(null);
        repository = mock(CartRepository.class);
        catalog = mock(CatalogLookup.class);
        stock = mock(StockLookup.class);
        when(repository.findByUserId("user-1")).thenAnswer(invocation -> Optional.ofNullable(stored.get()));
        when(repository.save(any(Cart.class))).thenAnswer(invocation -> {
            stored.set(invocation.getArgument(0));
            return invocation.getArgument(0);
        });
        CatalogProduct spinach = new CatalogProduct("p1", "baby-spinach", "Baby Spinach", 17900, null, "250 g", "Nilgiris", true);
        when(catalog.requirePurchasable("p1")).thenReturn(spinach);
        when(catalog.findByIds(any())).thenReturn(Map.of("p1", spinach));
        when(stock.availableFor(any())).thenReturn(StockSnapshot.of(Map.of("p1", 3)));
        service = new CartService(repository, catalog, stock);
    }

    @Test
    void addItemRejectsAQuantityThatExceedsWhatIsAlreadyInTheBasket() {
        service.addItem("user-1", null, "p1", 2);

        IllegalArgumentException rejected = assertThrows(IllegalArgumentException.class,
                () -> service.addItem("user-1", null, "p1", 2));

        assertEquals("Only 3 available.", rejected.getMessage());
        assertEquals(2, stored.get().getItems().getFirst().qty());
    }

    @Test
    void updateToZeroRemovesTheLine() {
        service.addItem("user-1", null, "p1", 1);

        CartResponse cart = service.updateQty("user-1", null, "p1", 0);

        assertEquals(0, cart.itemCount());
        assertEquals(0, cart.subtotalPaise());
    }

    @Test
    void mergeAddsGuestQuantitiesOntoTheAccountBasket() {
        service.addItem("user-1", null, "p1", 1);
        Cart guest = new Cart();
        guest.setGuestToken("guest-1");
        guest.setItems(java.util.List.of(new Cart.Item("p1", 1)));
        when(repository.findByGuestToken("guest-1")).thenReturn(Optional.of(guest));

        CartResponse cart = service.merge("user-1", "guest-1");

        assertEquals(2, cart.items().getFirst().qty());
        assertEquals(35800L, cart.subtotalPaise());
        verify(repository).delete(guest);
    }

    @Test
    void mergeLeavesTheAccountBasketAloneWhenThereIsNoGuestCart() {
        service.addItem("user-1", null, "p1", 1);
        when(repository.findByGuestToken("guest-1")).thenReturn(Optional.empty());

        CartResponse cart = service.merge("user-1", "guest-1");

        assertEquals(1, cart.itemCount());
        verify(repository, never()).delete(any(Cart.class));
    }
}
