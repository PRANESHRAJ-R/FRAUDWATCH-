package com.fraudwatch.controller;

import com.fraudwatch.entity.FlaggedTransaction;
import com.fraudwatch.entity.Transaction;
import com.fraudwatch.service.TransactionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    public static class TransactionRequest {
        @NotNull(message = "Sender account ID cannot be null")
        private Long senderAccountId;

        @NotNull(message = "Receiver account ID cannot be null")
        private Long receiverAccountId;

        @NotNull(message = "Amount cannot be null")
        @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
        private BigDecimal amount;

        public Long getSenderAccountId() {
            return senderAccountId;
        }

        public void setSenderAccountId(Long senderAccountId) {
            this.senderAccountId = senderAccountId;
        }

        public Long getReceiverAccountId() {
            return receiverAccountId;
        }

        public void setReceiverAccountId(Long receiverAccountId) {
            this.receiverAccountId = receiverAccountId;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }
    }

    public static class TransactionResponse {
        private Long id;
        private String transactionReference;
        private Long senderAccountId;
        private String senderAccountNumber;
        private String senderOwnerName;
        private Long receiverAccountId;
        private String receiverAccountNumber;
        private String receiverOwnerName;
        private BigDecimal amount;
        private String timestamp;
        private String status;
        private List<FlaggedTransaction> flags;

        public TransactionResponse(Transaction tx, List<FlaggedTransaction> flags) {
            this.id = tx.getId();
            this.transactionReference = tx.getTransactionReference();
            if (tx.getSender() != null) {
                this.senderAccountId = tx.getSender().getId();
                this.senderAccountNumber = tx.getSender().getAccountNumber();
                this.senderOwnerName = tx.getSender().getOwnerName();
            }
            if (tx.getReceiver() != null) {
                this.receiverAccountId = tx.getReceiver().getId();
                this.receiverAccountNumber = tx.getReceiver().getAccountNumber();
                this.receiverOwnerName = tx.getReceiver().getOwnerName();
            }
            this.amount = tx.getAmount();
            this.timestamp = tx.getTimestamp() != null ? tx.getTimestamp().toString() : null;
            this.status = tx.getStatus() != null ? tx.getStatus().name() : null;
            this.flags = flags != null ? flags : new ArrayList<>();
        }

        public Long getId() {
            return id;
        }

        public String getTransactionReference() {
            return transactionReference;
        }

        public Long getSenderAccountId() {
            return senderAccountId;
        }

        public String getSenderAccountNumber() {
            return senderAccountNumber;
        }

        public String getSenderOwnerName() {
            return senderOwnerName;
        }

        public Long getReceiverAccountId() {
            return receiverAccountId;
        }

        public String getReceiverAccountNumber() {
            return receiverAccountNumber;
        }

        public String getReceiverOwnerName() {
            return receiverOwnerName;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public String getStatus() {
            return status;
        }

        public List<FlaggedTransaction> getFlags() {
            return flags;
        }
    }

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    public ResponseEntity<TransactionResponse> createTransaction(@Valid @RequestBody TransactionRequest request) {
        Transaction tx = transactionService.createTransaction(
                request.getSenderAccountId(),
                request.getReceiverAccountId(),
                request.getAmount()
        );
        List<FlaggedTransaction> flags = transactionService.getFlagsForTransaction(tx.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(new TransactionResponse(tx, flags));
    }

    @GetMapping
    public ResponseEntity<List<TransactionResponse>> getAllTransactions(
            @RequestParam(required = false) Transaction.TransactionStatus status) {
        List<Transaction> list = (status != null)
                ? transactionService.getTransactionsByStatus(status)
                : transactionService.getAllTransactions();

        List<TransactionResponse> responses = new ArrayList<>();
        for (Transaction tx : list) {
            List<FlaggedTransaction> flags = transactionService.getFlagsForTransaction(tx.getId());
            responses.add(new TransactionResponse(tx, flags));
        }
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponse> getTransactionById(@PathVariable Long id) {
        Transaction tx = transactionService.getTransactionById(id);
        List<FlaggedTransaction> flags = transactionService.getFlagsForTransaction(tx.getId());
        return ResponseEntity.ok(new TransactionResponse(tx, flags));
    }

    @GetMapping("/flagged")
    public ResponseEntity<List<TransactionResponse>> getFlaggedTransactions() {
        List<Transaction> list = transactionService.getTransactionsByStatus(Transaction.TransactionStatus.FLAGGED);
        List<TransactionResponse> responses = new ArrayList<>();
        for (Transaction tx : list) {
            List<FlaggedTransaction> flags = transactionService.getFlagsForTransaction(tx.getId());
            responses.add(new TransactionResponse(tx, flags));
        }
        return ResponseEntity.ok(responses);
    }
}
