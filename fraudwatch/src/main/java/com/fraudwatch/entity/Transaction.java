package com.fraudwatch.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
public class Transaction {

    public enum TransactionStatus {
        PENDING,
        COMPLETED,
        FLAGGED,
        APPROVED,
        BLOCKED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Transaction reference cannot be blank")
    @Column(name = "transaction_reference", nullable = false, unique = true, length = 64)
    private String transactionReference;

    @NotNull(message = "Sender cannot be null")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sender_id", nullable = false)
    private Account sender;

    @NotNull(message = "Receiver cannot be null")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "receiver_id", nullable = false)
    private Account receiver;

    @NotNull(message = "Amount cannot be null")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @NotNull(message = "Timestamp cannot be null")
    @Column(nullable = false)
    private LocalDateTime timestamp;

    @NotNull(message = "Status cannot be null")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status;

    public Transaction() {}

    public Transaction(String transactionReference, Account sender, Account receiver, BigDecimal amount, LocalDateTime timestamp, TransactionStatus status) {
        this.transactionReference = transactionReference;
        this.sender = sender;
        this.receiver = receiver;
        this.amount = amount;
        this.timestamp = timestamp;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public void setTransactionReference(String transactionReference) {
        this.transactionReference = transactionReference;
    }

    public Account getSender() {
        return sender;
    }

    public void setSender(Account sender) {
        this.sender = sender;
    }

    public Account getReceiver() {
        return receiver;
    }

    public void setReceiver(Account receiver) {
        this.receiver = receiver;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }
}
