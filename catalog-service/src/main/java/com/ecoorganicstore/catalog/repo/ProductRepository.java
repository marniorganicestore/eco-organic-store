package com.ecoorganicstore.catalog.repo;

import com.ecoorganicstore.catalog.domain.Product;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface ProductRepository extends MongoRepository<Product, String> {
    Optional<Product> findBySlug(String slug);
    Page<Product> findByFeaturedTrueAndActiveTrue(Pageable pageable);
    Page<Product> findByCategoryIdAndActiveTrue(String categoryId, Pageable pageable);
    Page<Product> findByActiveTrue(Pageable pageable);
    Page<Product> findByNameContainingIgnoreCaseAndActiveTrue(String q, Pageable pageable);
    @Query("{ $or: [ { name: { $regex: ?0, $options: 'i' } }, { slug: { $regex: ?0, $options: 'i' } }, { origin: { $regex: ?0, $options: 'i' } } ] }")
    Page<Product> searchByText(String pattern, Pageable pageable);
    long countByCategoryId(String categoryId);
}