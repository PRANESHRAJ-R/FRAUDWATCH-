package com.fraudwatch.controller;

import com.fraudwatch.entity.ReviewOutcome;
import com.fraudwatch.service.ReviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    public static class ReviewActionRequest {
        private String reviewer;
        private String reason;

        public String getReviewer() {
            return reviewer;
        }

        public void setReviewer(String reviewer) {
            this.reviewer = reviewer;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/pending")
    public ResponseEntity<List<ReviewService.PendingReviewItem>> getPendingReviews() {
        return ResponseEntity.ok(reviewService.getPendingReviews());
    }

    @PostMapping("/{transactionId}/approve")
    public ResponseEntity<ReviewOutcome> approveTransaction(
            @PathVariable Long transactionId,
            @RequestBody(required = false) ReviewActionRequest request) {
        String reviewer = request != null ? request.getReviewer() : "Reviewer";
        String reason = request != null ? request.getReason() : "Approved by reviewer";
        ReviewOutcome outcome = reviewService.approveTransaction(transactionId, reviewer, reason);
        return ResponseEntity.ok(outcome);
    }

    @PostMapping("/{transactionId}/block")
    public ResponseEntity<ReviewOutcome> blockTransaction(
            @PathVariable Long transactionId,
            @RequestBody(required = false) ReviewActionRequest request) {
        String reviewer = request != null ? request.getReviewer() : "Reviewer";
        String reason = request != null ? request.getReason() : "Blocked by reviewer";
        ReviewOutcome outcome = reviewService.blockTransaction(transactionId, reviewer, reason);
        return ResponseEntity.ok(outcome);
    }
}
