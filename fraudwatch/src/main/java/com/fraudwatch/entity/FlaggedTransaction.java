package com.fraudwatch.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "flagged_transactions",
    uniqueConstraints = @UniqueConstraint(name = "uk_tx_rule", columnNames = {"transaction_id", "rule_id"})
)
public class FlaggedTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Transaction cannot be null")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @NotNull(message = "Rule cannot be null")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "rule_id", nullable = false)
    private Rule rule;

    @NotBlank(message = "Reason cannot be blank")
    @Column(nullable = false, length = 255)
    private String reason;

    @NotNull(message = "FlaggedAt cannot be null")
    @Column(name = "flagged_at", nullable = false)
    private LocalDateTime flaggedAt;

    @Column(nullable = false)
    private boolean resolved = false;

    public FlaggedTransaction() {}

    public FlaggedTransaction(Transaction transaction, Rule rule, String reason, LocalDateTime flaggedAt, boolean resolved) {
        this.transaction = transaction;
        this.rule = rule;
        this.reason = reason;
        this.flaggedAt = flaggedAt;
        this.resolved = resolved;
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

    public Rule getRule() {
        return rule;
    }

    public void setRule(Rule rule) {
        this.rule = rule;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDateTime getFlaggedAt() {
        return flaggedAt;
    }

    public void setFlaggedAt(LocalDateTime flaggedAt) {
        this.flaggedAt = flaggedAt;
    }

    public boolean isResolved() {
        return resolved;
    }

    public void setResolved(boolean resolved) {
        this.resolved = resolved;
    }
}
