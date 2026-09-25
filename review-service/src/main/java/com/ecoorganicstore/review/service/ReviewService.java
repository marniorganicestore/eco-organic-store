package com.ecoorganicstore.review.service;

import com.ecoorganicstore.common.web.PageWindow;
import com.ecoorganicstore.review.domain.Review;
import com.ecoorganicstore.review.repo.ReviewRepository;
import com.ecoorganicstore.common.web.UnauthorizedException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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

    public Page<Review> byProduct(String productId, int page, int size) {
        return reviewRepository.findByProductIdAndStatus(productId, "VISIBLE", newest(page, size, PageWindow.REVIEW_SIZE));
    }

    public Page<Review> forAdmin(String status, int page, int size) {
        var pageable = newest(page, size, PageWindow.ADMIN_SIZE);
        if (status == null || status.isBlank()) return reviewRepository.findAll(pageable);
        return reviewRepository.findByStatus(normalizeStatus(status), pageable);
    }

    public long hiddenCount() {
        return reviewRepository.countByStatus("HIDDEN");
    }

    public Review setStatus(String reviewId, String status) {
        Review review = reviewRepository.findById(reviewId).orElseThrow(() -> new IllegalArgumentException("Review not found"));
        review.setStatus(normalizeStatus(status));
        review = reviewRepository.save(review);
        recalculate(review.getProductId());
        return review;
    }

    private static PageRequest newest(int page, int size, int fallback) {
        return PageRequest.of(
                PageWindow.page(page),
                PageWindow.size(size, fallback),
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
    }

    private static String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("Choose a review status.");
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if (!"VISIBLE".equals(normalized) && !"HIDDEN".equals(normalized)) {
            throw new IllegalArgumentException("Unknown review status.");
        }
        return normalized;
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