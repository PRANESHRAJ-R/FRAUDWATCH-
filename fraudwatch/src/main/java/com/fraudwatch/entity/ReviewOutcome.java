package com.fraudwatch.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Table(name = "review_outcomes")
public class ReviewOutcome {

    public enum Outcome {
        APPROVED,
        BLOCKED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Transaction cannot be null")
    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "transaction_id", nullable = false, unique = true)
    private Transaction transaction;

    @NotNull(message = "Outcome cannot be null")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Outcome outcome;

    @NotBlank(message = "Reviewer cannot be blank")
    @Column(nullable = false, length = 100)
    private String reviewer;

    @Column(length = 255)
    private String reason;

    @NotNull(message = "ReviewedAt cannot be null")
    @Column(name = "reviewed_at", nullable = false)
    private LocalDateTime reviewedAt;

    public ReviewOutcome() {}

    public ReviewOutcome(Transaction transaction, Outcome outcome, String reviewer, String reason, LocalDateTime reviewedAt) {
        this.transaction = transaction;
        this.outcome = outcome;
        this.reviewer = reviewer;
        this.reason = reason;
        this.reviewedAt = reviewedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public void setTransaction(Transaction transaction) {
        this.transaction = transaction;
    }

    public Outcome getOutcome() {
        return outcome;
    }

    public void setOutcome(Outcome outcome) {
        this.outcome = outcome;
    }

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

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }
}
