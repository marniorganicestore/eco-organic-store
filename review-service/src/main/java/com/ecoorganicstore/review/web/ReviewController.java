package com.ecoorganicstore.review.web;

import com.ecoorganicstore.common.security.AuthGuards;
import com.ecoorganicstore.common.security.UserContextResolver;
import com.ecoorganicstore.review.domain.Review;
import com.ecoorganicstore.review.service.ReviewService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping
public class ReviewController {
    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/api/products/{productId}/reviews")
    public List<Review> productReviews(@PathVariable String productId) {
        return reviewService.byProduct(productId);
    }

    @PostMapping("/api/reviews")
    public Review create(HttpServletRequest request, @RequestBody ReviewRequest reviewRequest) {
        String userId = UserContextResolver.fromHeaders(request).userId();
        return reviewService.create(userId, reviewRequest.productId(), reviewRequest.rating(), reviewRequest.body());
    }

    @GetMapping("/api/admin/reviews")
    public List<ReviewResponse> queue(HttpServletRequest request, @RequestParam(required = false) String status) {
        AuthGuards.requireAdmin(request);
        return reviewService.forAdmin(status).stream().map(ReviewController::toResponse).toList();
    }

    @PatchMapping("/api/admin/reviews/{reviewId}")
    public ReviewResponse update(HttpServletRequest request, @PathVariable String reviewId,
                                 @Valid @RequestBody StatusRequest statusRequest) {
        AuthGuards.requireAdmin(request);
        return toResponse(reviewService.setStatus(reviewId, statusRequest.status()));
    }

    private static ReviewResponse toResponse(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getUserId(),
                review.getProductId(),
                review.getRating(),
                review.getBody(),
                review.isVerifiedPurchase(),
                review.getStatus(),
                review.getCreatedAt());
    }

    public record ReviewRequest(String productId, int rating, String body) {}
    public record StatusRequest(@NotBlank(message = "Choose a review status.") String status) {}
    public record ReviewResponse(
            String id,
            String userId,
            String productId,
            int rating,
            String body,
            boolean verifiedPurchase,
            String status,
            Instant createdAt) {}
}