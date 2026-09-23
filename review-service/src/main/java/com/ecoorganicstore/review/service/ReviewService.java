package com.ecoorganicstore.review.service;

import com.ecoorganicstore.review.domain.Review;
import com.ecoorganicstore.review.repo.ReviewRepository;
import com.ecoorganicstore.common.web.UnauthorizedException;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final RestClient restClient;
    private final String internalKey;

    @Value("${services.order:http://localhost:8085}")
    private String orderUrl;
    @Value("${services.catalog:http://localhost:8082}")
    private String catalogUrl;

    public ReviewService(ReviewRepository reviewRepository, RestClient restClient,
                         @Value("${app.internal-key}") String internalKey) {
        this.reviewRepository = reviewRepository;
        this.restClient = restClient;
        this.internalKey = internalKey;
    }

    public Review create(String userId, String productId, int rating, String body) {
        if (userId == null || userId.isBlank()) throw new UnauthorizedException("Authentication required");
        Map purchase = restClient.get().uri(orderUrl + "/internal/orders/" + userId + "/purchased/" + productId)
                .header("X-Internal-Key", internalKey).retrieve().body(Map.class);
        boolean verified = Boolean.TRUE.equals(purchase.get("purchased"));
        if (!verified) throw new IllegalArgumentException("Only verified buyers can review this product");

        Review review = new Review();
        review.setUserId(userId);
        review.setProductId(productId);
        review.setRating(rating);
        review.setBody(body);
        review.setVerifiedPurchase(true);
        review = reviewRepository.save(review);
        recalculate(productId);
        return review;
    }

    public List<Review> byProduct(String productId) {
        return reviewRepository.findByProductIdAndStatus(productId, "VISIBLE");
    }

    public List<Review> hiddenQueue() {
        return reviewRepository.findByStatus("HIDDEN");
    }

    public Review setStatus(String reviewId, String status) {
        Review review = reviewRepository.findById(reviewId).orElseThrow(() -> new IllegalArgumentException("Review not found"));
        review.setStatus(status);
        review = reviewRepository.save(review);
        recalculate(review.getProductId());
        return review;
    }

    private void recalculate(String productId) {
        List<Review> visible = reviewRepository.findByProductIdAndStatus(productId, "VISIBLE");
        long count = visible.size();
        double avg = count == 0 ? 0 : visible.stream().mapToInt(Review::getRating).average().orElse(0);
        restClient.patch().uri(catalogUrl + "/internal/products/" + productId + "/rating")
                .header("X-Internal-Key", internalKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("averageRating", avg, "reviewCount", count))
                .retrieve().toBodilessEntity();
    }
}