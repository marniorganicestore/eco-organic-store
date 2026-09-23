package com.ecoorganicstore.catalog.repo;

import com.ecoorganicstore.catalog.domain.Product;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProductRepository extends MongoRepository<Product, String> {
    Optional<Product> findBySlug(String slug);
    List<Product> findByFeaturedTrueAndActiveTrue();
    List<Product> findByCategoryIdAndActiveTrue(String categoryId);
    List<Product> findByActiveTrue();
    List<Product> findByNameContainingIgnoreCaseAndActiveTrue(String q);
}