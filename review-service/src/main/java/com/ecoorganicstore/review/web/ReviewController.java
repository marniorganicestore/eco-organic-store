package com.ecoorganicstore.review.web;

import com.ecoorganicstore.common.security.AuthGuards;
import com.ecoorganicstore.common.security.UserContextResolver;
import com.ecoorganicstore.review.domain.Review;
import com.ecoorganicstore.review.service.ReviewService;
import jakarta.servlet.http.HttpServletRequest;
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
    public List<Review> queue(HttpServletRequest request) {
        AuthGuards.requireAdmin(request);
        return reviewService.hiddenQueue();
    }

    @PatchMapping("/api/admin/reviews/{reviewId}")
    public Review update(HttpServletRequest request, @PathVariable String reviewId, @RequestBody StatusRequest statusRequest) {
        AuthGuards.requireAdmin(request);
        return reviewService.setStatus(reviewId, statusRequest.status());
    }

    public record ReviewRequest(String productId, int rating, String body) {}
    public record StatusRequest(String status) {}
}