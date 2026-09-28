package com.fraudwatch.controller;

import com.fraudwatch.entity.Transaction;
import com.fraudwatch.repository.FlaggedTransactionRepository;
import com.fraudwatch.repository.TransactionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    public static class SummaryResponse {
        private long totalTransactions;
        private long completedTransactions;
        private long flaggedTransactions;
        private long approvedTransactions;
        private long blockedTransactions;
        private long pendingReviews;

        public SummaryResponse(long totalTransactions, long completedTransactions,
                               long flaggedTransactions, long approvedTransactions,
                               long blockedTransactions, long pendingReviews) {
            this.totalTransactions = totalTransactions;
            this.completedTransactions = completedTransactions;
            this.flaggedTransactions = flaggedTransactions;
            this.approvedTransactions = approvedTransactions;
            this.blockedTransactions = blockedTransactions;
            this.pendingReviews = pendingReviews;
        }

        public long getTotalTransactions() {
            return totalTransactions;
        }

        public long getCompletedTransactions() {
            return completedTransactions;
        }

        public long getFlaggedTransactions() {
            return flaggedTransactions;
        }

        public long getApprovedTransactions() {
            return approvedTransactions;
        }

        public long getBlockedTransactions() {
            return blockedTransactions;
        }

        public long getPendingReviews() {
            return pendingReviews;
        }
    }

    private final TransactionRepository transactionRepository;
    private final FlaggedTransactionRepository flaggedTransactionRepository;

    public DashboardController(TransactionRepository transactionRepository,
                               FlaggedTransactionRepository flaggedTransactionRepository) {
        this.transactionRepository = transactionRepository;
        this.flaggedTransactionRepository = flaggedTransactionRepository;
    }

    @GetMapping("/summary")
    public ResponseEntity<SummaryResponse> getSummary() {
        long total = transactionRepository.count();
        long completed = transactionRepository.countByStatus(Transaction.TransactionStatus.COMPLETED);
        long flagged = transactionRepository.countByStatus(Transaction.TransactionStatus.FLAGGED);
        long approved = transactionRepository.countByStatus(Transaction.TransactionStatus.APPROVED);
        long blocked = transactionRepository.countByStatus(Transaction.TransactionStatus.BLOCKED);
        long pendingReviews = flagged; // currently flagged transactions waiting for review

        return ResponseEntity.ok(new SummaryResponse(
                total, completed, flagged, approved, blocked, pendingReviews
        ));
    }

    @GetMapping("/flagged-by-rule")
    public ResponseEntity<Map<String, Long>> getFlaggedByRule() {
        Map<String, Long> result = new HashMap<>();
        long amountThresholdCount = flaggedTransactionRepository.countByRuleCode("AMOUNT_THRESHOLD");
        long velocityCount = flaggedTransactionRepository.countByRuleCode("VELOCITY");

        result.put("AMOUNT_THRESHOLD", amountThresholdCount);
        result.put("VELOCITY", velocityCount);
        return ResponseEntity.ok(result);
    }
}
