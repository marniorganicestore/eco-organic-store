package com.ecoorganicstore.order.repo;

import com.ecoorganicstore.order.domain.Order;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface OrderRepository extends MongoRepository<Order, String> {
    Optional<Order> findByOrderNumber(String orderNumber);

    List<Order> findByUserId(String userId);

    Page<Order> findByUserId(String userId, Pageable pageable);

    Page<Order> findByOrderStatus(String orderStatus, Pageable pageable);

    // A derived GreaterThanEqualAndLessThan method writes createdAt twice, which MongoDB rejects.
    @Query(value = "{ 'createdAt': { $gte: ?0, $lt: ?1 } }", sort = "{ 'createdAt': -1 }")
    List<Order> findCreatedFromInclusiveUntil(Instant startInclusive, Instant endExclusive);
}