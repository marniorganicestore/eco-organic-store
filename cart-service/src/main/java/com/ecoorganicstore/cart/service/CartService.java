package com.ecoorganicstore.cart.service;

import com.ecoorganicstore.cart.domain.Cart;
import com.ecoorganicstore.cart.repo.CartRepository;
import com.ecoorganicstore.cart.web.CartResponse;
import com.ecoorganicstore.cart.web.InternalCartResponse;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class CartService {
    public static final int MAX_QTY = 24;

    private final CartRepository cartRepository;
    private final CatalogLookup catalogLookup;
    private final StockLookup stockLookup;

    public CartService(CartRepository cartRepository, CatalogLookup catalogLookup, StockLookup stockLookup) {
        this.cartRepository = cartRepository;
        this.catalogLookup = catalogLookup;
        this.stockLookup = stockLookup;
    }

    public CartResponse view(String userId, String guestToken) {
        return present(load(userId, guestToken));
    }

    public CartResponse addItem(String userId, String guestToken, String productId, int qty) {
        String id = requireProductId(productId);
        if (qty < 1) throw new IllegalArgumentException("Add at least 1.");
        Cart cart = load(userId, guestToken);
        LinkedHashMap<String, Integer> lines = linesOf(cart);
        int next = lines.getOrDefault(id, 0) + qty;
        requireWithinMax(next);
        catalogLookup.requirePurchasable(id);
        ensureStock(id, next);
        lines.put(id, next);
        return present(save(cart, lines));
    }

    public CartResponse updateQty(String userId, String guestToken, String productId, int qty) {
        String id = requireProductId(productId);
        if (qty < 0) throw new IllegalArgumentException("Quantity cannot be negative.");
        Cart cart = load(userId, guestToken);
        LinkedHashMap<String, Integer> lines = linesOf(cart);
        if (qty == 0) {
            lines.remove(id);
            return present(save(cart, lines));
        }
        requireWithinMax(qty);
        catalogLookup.requirePurchasable(id);
        ensureStock(id, qty);
        lines.put(id, qty);
        return present(save(cart, lines));
    }

    public CartResponse merge(String userId, String guestToken) {
        if (userId == null || userId.isBlank() || guestToken == null || guestToken.isBlank()) {
            throw new IllegalArgumentException("Sign in again to keep your basket.");
        }
        Cart user = cartRepository.findByUserId(userId).orElseGet(() -> createUserCart(userId));
        Optional<Cart> guest = cartRepository.findByGuestToken(guestToken);
        if (guest.isEmpty()) return present(user);
        LinkedHashMap<String, Integer> lines = linesOf(user);
        for (var entry : linesOf(guest.get()).entrySet()) {
            int combined = lines.getOrDefault(entry.getKey(), 0) + entry.getValue();
            lines.put(entry.getKey(), Math.min(MAX_QTY, combined));
        }
        Cart saved = save(user, lines);
        cartRepository.delete(guest.get());
        return present(saved);
    }

    public void clear(String userId) {
        cartRepository.findByUserId(userId).ifPresent(cart -> save(cart, new LinkedHashMap<>()));
    }

    public InternalCartResponse internalLines(String userId) {
        List<InternalCartResponse.Line> lines = linesOf(load(userId, null)).entrySet().stream()
                .map(entry -> new InternalCartResponse.Line(entry.getKey(), entry.getValue()))
                .toList();
        return new InternalCartResponse(lines);
    }

    private CartResponse present(Cart cart) {
        List<Cart.Item> items = cart.getItems() == null ? List.of() : cart.getItems();
        if (items.isEmpty()) return CartResponse.empty();
        List<String> ids = items.stream().map(Cart.Item::productId).distinct().toList();
        return CartViews.compose(items, catalogLookup.findByIds(ids), stockLookup.availableFor(ids));
    }

    private void ensureStock(String productId, int qty) {
        StockSnapshot snapshot = stockLookup.availableFor(List.of(productId));
        if (!snapshot.known()) {
            throw new IllegalArgumentException("We could not confirm stock. Try again.");
        }
        int available = snapshot.available().getOrDefault(productId, 0);
        if (available <= 0) throw new IllegalArgumentException("This item is out of stock.");
        if (qty > available) throw new IllegalArgumentException("Only " + available + " available.");
    }

    private Cart load(String userId, String guestToken) {
        if (userId != null && !userId.isBlank()) {
            return cartRepository.findByUserId(userId).orElseGet(() -> createUserCart(userId));
        }
        if (guestToken == null || guestToken.isBlank()) {
            throw new IllegalArgumentException("Guest token is required for guest cart");
        }
        return cartRepository.findByGuestToken(guestToken).orElseGet(() -> createGuestCart(guestToken));
    }

    private Cart createUserCart(String userId) {
        Cart cart = new Cart();
        cart.setUserId(userId);
        return cartRepository.save(cart);
    }

    private Cart createGuestCart(String guestToken) {
        Cart cart = new Cart();
        cart.setGuestToken(guestToken);
        return cartRepository.save(cart);
    }

    private Cart save(Cart cart, LinkedHashMap<String, Integer> lines) {
        cart.setItems(lines.entrySet().stream()
                .filter(entry -> entry.getValue() != null && entry.getValue() > 0)
                .map(entry -> new Cart.Item(entry.getKey(), entry.getValue()))
                .toList());
        cart.setUpdatedAt(Instant.now());
        return cartRepository.save(cart);
    }

    private static LinkedHashMap<String, Integer> linesOf(Cart cart) {
        LinkedHashMap<String, Integer> lines = new LinkedHashMap<>();
        if (cart.getItems() == null) return lines;
        for (Cart.Item item : cart.getItems()) {
            if (item != null && item.productId() != null && !item.productId().isBlank() && item.qty() > 0) {
                lines.put(item.productId(), item.qty());
            }
        }
        return lines;
    }

    private static String requireProductId(String productId) {
        if (productId == null || productId.isBlank()) throw new IllegalArgumentException("Choose a product.");
        return productId.trim();
    }

    private static void requireWithinMax(int qty) {
        if (qty > MAX_QTY) throw new IllegalArgumentException("You can add up to " + MAX_QTY + " of one item.");
    }
}
