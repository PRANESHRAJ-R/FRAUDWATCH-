package com.fraudwatch.service;

import com.fraudwatch.entity.Account;
import com.fraudwatch.entity.FlaggedTransaction;
import com.fraudwatch.entity.ReviewOutcome;
import com.fraudwatch.entity.Transaction;
import com.fraudwatch.exception.ConflictException;
import com.fraudwatch.exception.InsufficientBalanceException;
import com.fraudwatch.exception.NotFoundException;
import com.fraudwatch.repository.AccountRepository;
import com.fraudwatch.repository.FlaggedTransactionRepository;
import com.fraudwatch.repository.ReviewOutcomeRepository;
import com.fraudwatch.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ReviewService {

    public static class PendingReviewItem {
        private Transaction transaction;
        private List<FlaggedTransaction> flags;

        public PendingReviewItem(Transaction transaction, List<FlaggedTransaction> flags) {
            this.transaction = transaction;
            this.flags = flags;
        }

        public Transaction getTransaction() {
            return transaction;
        }

        public List<FlaggedTransaction> getFlags() {
            return flags;
        }
    }

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final FlaggedTransactionRepository flaggedTransactionRepository;
    private final ReviewOutcomeRepository reviewOutcomeRepository;

    public ReviewService(TransactionRepository transactionRepository,
                         AccountRepository accountRepository,
                         FlaggedTransactionRepository flaggedTransactionRepository,
                         ReviewOutcomeRepository reviewOutcomeRepository) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.flaggedTransactionRepository = flaggedTransactionRepository;
        this.reviewOutcomeRepository = reviewOutcomeRepository;
    }

    public List<PendingReviewItem> getPendingReviews() {
        List<Transaction> flaggedList = transactionRepository.findByStatus(Transaction.TransactionStatus.FLAGGED);
        List<PendingReviewItem> result = new ArrayList<>();
        for (Transaction tx : flaggedList) {
            List<FlaggedTransaction> flags = flaggedTransactionRepository.findByTransactionId(tx.getId());
            result.add(new PendingReviewItem(tx, flags));
        }
        return result;
    }

    @Transactional
    public ReviewOutcome approveTransaction(Long transactionId, String reviewer, String reason) {
        Transaction tx = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new NotFoundException("Transaction not found with ID: " + transactionId));

        if (tx.getStatus() != Transaction.TransactionStatus.FLAGGED) {
            throw new ConflictException("Cannot approve transaction with status " + tx.getStatus() +
                    ". Only FLAGGED transactions can be approved.");
        }

        if (reviewOutcomeRepository.existsByTransactionId(transactionId)) {
            throw new ConflictException("Transaction has already been reviewed");
        }

        Account sender = tx.getSender();
        Account receiver = tx.getReceiver();

        if (sender.getBalance().compareTo(tx.getAmount()) < 0) {
            throw new InsufficientBalanceException("Sender balance is no longer sufficient. Balance: "
                    + sender.getBalance() + ", Amount: " + tx.getAmount());
        }

        // Debit sender and credit receiver
        sender.setBalance(sender.getBalance().subtract(tx.getAmount()));
        receiver.setBalance(receiver.getBalance().add(tx.getAmount()));
        accountRepository.save(sender);
        accountRepository.save(receiver);

        // Update transaction status
        tx.setStatus(Transaction.TransactionStatus.APPROVED);
        transactionRepository.save(tx);

        // Mark flagged records as resolved
        List<FlaggedTransaction> flags = flaggedTransactionRepository.findByTransactionId(transactionId);
        for (FlaggedTransaction f : flags) {
            f.setResolved(true);
        }
        flaggedTransactionRepository.saveAll(flags);

        // Create ReviewOutcome
        String effectiveReviewer = (reviewer != null && !reviewer.trim().isEmpty()) ? reviewer.trim() : "Reviewer";
        ReviewOutcome outcome = new ReviewOutcome(
                tx,
                ReviewOutcome.Outcome.APPROVED,
                effectiveReviewer,
                reason,
                LocalDateTime.now()
        );
        return reviewOutcomeRepository.save(outcome);
    }

    @Transactional
    public ReviewOutcome blockTransaction(Long transactionId, String reviewer, String reason) {
        Transaction tx = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new NotFoundException("Transaction not found with ID: " + transactionId));

        if (tx.getStatus() != Transaction.TransactionStatus.FLAGGED) {
            throw new ConflictException("Cannot block transaction with status " + tx.getStatus() +
                    ". Only FLAGGED transactions can be blocked.");
        }

        if (reviewOutcomeRepository.existsByTransactionId(transactionId)) {
            throw new ConflictException("Transaction has already been reviewed");
        }

        // Mark transaction BLOCKED - DO NOT modify sender or receiver balance!
        tx.setStatus(Transaction.TransactionStatus.BLOCKED);
        transactionRepository.save(tx);

        // Mark flagged records as resolved
        List<FlaggedTransaction> flags = flaggedTransactionRepository.findByTransactionId(transactionId);
        for (FlaggedTransaction f : flags) {
            f.setResolved(true);
        }
        flaggedTransactionRepository.saveAll(flags);

        // Create ReviewOutcome
        String effectiveReviewer = (reviewer != null && !reviewer.trim().isEmpty()) ? reviewer.trim() : "Reviewer";
        ReviewOutcome outcome = new ReviewOutcome(
                tx,
                ReviewOutcome.Outcome.BLOCKED,
                effectiveReviewer,
                reason,
                LocalDateTime.now()
        );
        return reviewOutcomeRepository.save(outcome);
    }
}
