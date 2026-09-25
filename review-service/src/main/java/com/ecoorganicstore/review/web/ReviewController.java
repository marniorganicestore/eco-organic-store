package com.ecoorganicstore.review.web;

import com.ecoorganicstore.common.security.AuthGuards;
import com.ecoorganicstore.common.security.UserContextResolver;
import com.ecoorganicstore.common.web.PageResponse;
import com.ecoorganicstore.review.domain.Review;
import com.ecoorganicstore.review.service.ReviewService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping
public class ReviewController {
    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/api/products/{productId}/reviews")
    public PageResponse<Review> productReviews(@PathVariable String productId,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "8") int size) {
        return map(reviewService.byProduct(productId, page, size));
    }

    @PostMapping("/api/reviews")
    public Review create(HttpServletRequest request, @RequestBody ReviewRequest reviewRequest) {
        String userId = UserContextResolver.fromHeaders(request).userId();
        return reviewService.create(userId, reviewRequest.productId(), reviewRequest.rating(), reviewRequest.body());
    }

    @GetMapping("/api/admin/reviews/summary")
    public ReviewDeskResponse summary(HttpServletRequest request) {
        AuthGuards.requireAdmin(request);
        return new ReviewDeskResponse(reviewService.hiddenCount());
    }

    @GetMapping("/api/admin/reviews")
    public PageResponse<ReviewResponse> queue(HttpServletRequest request,
                                              @RequestParam(required = false) String status,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size) {
        AuthGuards.requireAdmin(request);
        Page<Review> reviews = reviewService.forAdmin(status, page, size);
        return PageResponse.of(reviews.getContent().stream().map(ReviewController::toResponse).toList(),
                reviews.getNumber(), reviews.getSize(), reviews.getTotalElements());
    }

    @PatchMapping("/api/admin/reviews/{reviewId}")
    public ReviewResponse update(HttpServletRequest request, @PathVariable String reviewId,
                                 @Valid @RequestBody StatusRequest statusRequest) {
        AuthGuards.requireAdmin(request);
        return toResponse(reviewService.setStatus(reviewId, statusRequest.status()));
    }

    private static PageResponse<Review> map(Page<Review> page) {
        return PageResponse.of(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements());
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

    public record ReviewDeskResponse(long hiddenCount) {}
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