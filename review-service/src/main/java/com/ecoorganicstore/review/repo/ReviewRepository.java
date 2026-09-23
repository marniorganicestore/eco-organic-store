package com.ecoorganicstore.review.repo;

import com.ecoorganicstore.review.domain.Review;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ReviewRepository extends MongoRepository<Review, String> {
    List<Review> findByProductIdAndStatus(String productId, String status);
    List<Review> findByStatus(String status);
}