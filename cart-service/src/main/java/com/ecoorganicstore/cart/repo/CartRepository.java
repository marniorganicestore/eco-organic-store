package com.ecoorganicstore.cart.repo;

import com.ecoorganicstore.cart.domain.Cart;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CartRepository extends MongoRepository<Cart, String> {
    Optional<Cart> findByUserId(String userId);
    Optional<Cart> findByGuestToken(String guestToken);
}