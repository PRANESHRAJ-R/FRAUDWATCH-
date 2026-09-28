package com.fraudwatch.service;

import com.fraudwatch.entity.Account;
import com.fraudwatch.entity.FlaggedTransaction;
import com.fraudwatch.entity.Transaction;
import com.fraudwatch.exception.BadRequestException;
import com.fraudwatch.exception.InsufficientBalanceException;
import com.fraudwatch.exception.NotFoundException;
import com.fraudwatch.repository.AccountRepository;
import com.fraudwatch.repository.FlaggedTransactionRepository;
import com.fraudwatch.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final FlaggedTransactionRepository flaggedTransactionRepository;
    private final FraudDetectionService fraudDetectionService;

    public TransactionService(TransactionRepository transactionRepository,
                              AccountRepository accountRepository,
                              FlaggedTransactionRepository flaggedTransactionRepository,
                              FraudDetectionService fraudDetectionService) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.flaggedTransactionRepository = flaggedTransactionRepository;
        this.fraudDetectionService = fraudDetectionService;
    }

    @Transactional
    public Transaction createTransaction(Long senderAccountId, Long receiverAccountId, BigDecimal amount) {
        // STEP 1: Validate request
        if (senderAccountId == null) {
            throw new BadRequestException("Sender account ID cannot be null");
        }
        if (receiverAccountId == null) {
            throw new BadRequestException("Receiver account ID cannot be null");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Amount must be greater than zero");
        }

        // STEP 2: Find sender
        Account sender = accountRepository.findById(senderAccountId)
                .orElseThrow(() -> new NotFoundException("Sender account not found with ID: " + senderAccountId));

        // STEP 3: Find receiver
        Account receiver = accountRepository.findById(receiverAccountId)
                .orElseThrow(() -> new NotFoundException("Receiver account not found with ID: " + receiverAccountId));

        // STEP 4: Make sure sender and receiver are different
        if (sender.getId().equals(receiver.getId())) {
            throw new BadRequestException("Sender and receiver accounts must be different");
        }

        // STEP 5: Make sure both accounts are active
        if (!sender.isActive()) {
            throw new BadRequestException("Sender account is inactive");
        }
        if (!receiver.isActive()) {
            throw new BadRequestException("Receiver account is inactive");
        }

        // STEP 6: Make sure amount is greater than zero
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Amount must be greater than zero");
        }

        // STEP 7: Make sure sender has enough balance
        if (sender.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException("Sender has insufficient balance: " + sender.getBalance());
        }

        LocalDateTime timestamp = LocalDateTime.now();
        String txRef = "TX-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();

        // STEP 8, 9, 10: Check EVERY enabled fraud rule and collect ALL triggered rules
        List<FraudDetectionService.FraudCheckResult> triggeredRules =
                fraudDetectionService.evaluate(sender, amount, timestamp);

        Transaction transaction = new Transaction();
        transaction.setTransactionReference(txRef);
        transaction.setSender(sender);
        transaction.setReceiver(receiver);
        transaction.setAmount(amount);
        transaction.setTimestamp(timestamp);

        if (triggeredRules.isEmpty()) {
            // No fraud rules triggered: debit sender, credit receiver, mark COMPLETED
            sender.setBalance(sender.getBalance().subtract(amount));
            receiver.setBalance(receiver.getBalance().add(amount));
            accountRepository.save(sender);
            accountRepository.save(receiver);

            transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
            return transactionRepository.save(transaction);
        } else {
            // One or more fraud rules triggered: save as FLAGGED, DO NOT modify balances
            transaction.setStatus(Transaction.TransactionStatus.FLAGGED);
            Transaction savedTx = transactionRepository.save(transaction);

            // Create FlaggedTransaction for EVERY triggered rule
            for (FraudDetectionService.FraudCheckResult check : triggeredRules) {
                FlaggedTransaction flag = new FlaggedTransaction(
                        savedTx,
                        check.getRule(),
                        check.getReason(),
                        timestamp,
                        false
                );
                flaggedTransactionRepository.save(flag);
            }

            return savedTx;
        }
    }

    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAllByOrderByTimestampDesc();
    }

    public Transaction getTransactionById(Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Transaction not found with ID: " + id));
    }

    public List<Transaction> getTransactionsByStatus(Transaction.TransactionStatus status) {
        return transactionRepository.findByStatus(status);
    }

    public List<FlaggedTransaction> getFlagsForTransaction(Long transactionId) {
        return flaggedTransactionRepository.findByTransactionId(transactionId);
    }
}
