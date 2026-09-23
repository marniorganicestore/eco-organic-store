package com.ecoorganicstore.inventory.repo;

import com.ecoorganicstore.inventory.domain.Stock;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface StockRepository extends MongoRepository<Stock, String> {
    Optional<Stock> findByProductId(String productId);
    List<Stock> findByOnHandLessThan(int onHand);
    List<Stock> findByProductIdIn(List<String> productIds);
}