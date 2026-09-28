package com.fraudwatch;

import com.fraudwatch.entity.Account;
import com.fraudwatch.entity.FlaggedTransaction;
import com.fraudwatch.entity.ReviewOutcome;
import com.fraudwatch.entity.Rule;
import com.fraudwatch.entity.Transaction;
import com.fraudwatch.exception.BadRequestException;
import com.fraudwatch.exception.ConflictException;
import com.fraudwatch.exception.InsufficientBalanceException;
import com.fraudwatch.repository.AccountRepository;
import com.fraudwatch.repository.FlaggedTransactionRepository;
import com.fraudwatch.repository.ReviewOutcomeRepository;
import com.fraudwatch.repository.TransactionRepository;
import com.fraudwatch.service.FraudDetectionService;
import com.fraudwatch.service.ReviewService;
import com.fraudwatch.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private FlaggedTransactionRepository flaggedTransactionRepository;

    @Mock
    private FraudDetectionService fraudDetectionService;

    @Mock
    private ReviewOutcomeRepository reviewOutcomeRepository;

    private TransactionService transactionService;
    private ReviewService reviewService;

    private Account sender;
    private Account receiver;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionService(
                transactionRepository,
                accountRepository,
                flaggedTransactionRepository,
                fraudDetectionService
        );

        reviewService = new ReviewService(
                transactionRepository,
                accountRepository,
                flaggedTransactionRepository,
                reviewOutcomeRepository
        );

        sender = new Account("ACC1001", "Sender User", new BigDecimal("100000.00"), "INR", true);
        sender.setId(1L);

        receiver = new Account("ACC1002", "Receiver User", new BigDecimal("50000.00"), "INR", true);
        receiver.setId(2L);
    }

    @Test
    @DisplayName("1. Normal transaction completes and updates balances")
    void testNormalTransactionCompletes() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(sender));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(receiver));
        when(fraudDetectionService.evaluate(eq(sender), any(BigDecimal.class), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));

        BigDecimal amount = new BigDecimal("5000.00");
        Transaction result = transactionService.createTransaction(1L, 2L, amount);

        assertNotNull(result);
        assertEquals(Transaction.TransactionStatus.COMPLETED, result.getStatus());
        assertEquals(new BigDecimal("95000.00"), sender.getBalance());
        assertEquals(new BigDecimal("55000.00"), receiver.getBalance());
        verify(accountRepository, times(1)).save(sender);
        verify(accountRepository, times(1)).save(receiver);
        verify(flaggedTransactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("2. Amount threshold flags transaction")
    void testAmountThresholdFlagsTransaction() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(sender));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(receiver));

        Rule amountRule = new Rule("Large Amount Rule", "AMOUNT_THRESHOLD", "Flags large amount", true,
                new BigDecimal("50000.00"), null, null);
        FraudDetectionService.FraudCheckResult check = new FraudDetectionService.FraudCheckResult(
                amountRule, "Transaction amount 60000.00 exceeds threshold 50000.00");

        when(fraudDetectionService.evaluate(eq(sender), any(BigDecimal.class), any(LocalDateTime.class)))
                .thenReturn(List.of(check));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));

        BigDecimal amount = new BigDecimal("60000.00");
        Transaction result = transactionService.createTransaction(1L, 2L, amount);

        assertNotNull(result);
        assertEquals(Transaction.TransactionStatus.FLAGGED, result.getStatus());
        verify(flaggedTransactionRepository, times(1)).save(any(FlaggedTransaction.class));
    }

    @Test
    @DisplayName("3. Velocity rule flags transaction")
    void testVelocityRuleFlagsTransaction() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(sender));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(receiver));

        Rule velocityRule = new Rule("Velocity Rule", "VELOCITY", "Rapid transfers", true,
                null, 3, 60);
        FraudDetectionService.FraudCheckResult check = new FraudDetectionService.FraudCheckResult(
                velocityRule, "Account ACC1001 exceeded velocity limit: 3 transactions in the last 60 seconds");

        when(fraudDetectionService.evaluate(eq(sender), any(BigDecimal.class), any(LocalDateTime.class)))
                .thenReturn(List.of(check));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));

        BigDecimal amount = new BigDecimal("1000.00");
        Transaction result = transactionService.createTransaction(1L, 2L, amount);

        assertNotNull(result);
        assertEquals(Transaction.TransactionStatus.FLAGGED, result.getStatus());
        verify(flaggedTransactionRepository, times(1)).save(any(FlaggedTransaction.class));
    }

    @Test
    @DisplayName("4. One transaction can trigger multiple rules simultaneously")
    void testOneTransactionCanTriggerMultipleRules() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(sender));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(receiver));

        Rule amountRule = new Rule("Large Amount Rule", "AMOUNT_THRESHOLD", "Flags large amount", true,
                new BigDecimal("50000.00"), null, null);
        Rule velocityRule = new Rule("Velocity Rule", "VELOCITY", "Rapid transfers", true,
                null, 3, 60);

        FraudDetectionService.FraudCheckResult check1 = new FraudDetectionService.FraudCheckResult(
                amountRule, "Amount exceeds 50000");
        FraudDetectionService.FraudCheckResult check2 = new FraudDetectionService.FraudCheckResult(
                velocityRule, "Velocity exceeded");

        when(fraudDetectionService.evaluate(eq(sender), any(BigDecimal.class), any(LocalDateTime.class)))
                .thenReturn(List.of(check1, check2));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));

        BigDecimal amount = new BigDecimal("60000.00");
        Transaction result = transactionService.createTransaction(1L, 2L, amount);

        assertNotNull(result);
        assertEquals(Transaction.TransactionStatus.FLAGGED, result.getStatus());
        verify(flaggedTransactionRepository, times(2)).save(any(FlaggedTransaction.class));
    }

    @Test
    @DisplayName("5. Flagged transaction does not modify balances")
    void testFlaggedTransactionDoesNotModifyBalances() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(sender));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(receiver));

        Rule rule = new Rule("Rule", "AMOUNT_THRESHOLD", "Desc", true, new BigDecimal("50000.00"), null, null);
        when(fraudDetectionService.evaluate(eq(sender), any(BigDecimal.class), any(LocalDateTime.class)))
                .thenReturn(List.of(new FraudDetectionService.FraudCheckResult(rule, "Reason")));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));

        BigDecimal initialSenderBalance = sender.getBalance();
        BigDecimal initialReceiverBalance = receiver.getBalance();

        transactionService.createTransaction(1L, 2L, new BigDecimal("60000.00"));

        assertEquals(initialSenderBalance, sender.getBalance());
        assertEquals(initialReceiverBalance, receiver.getBalance());
        verify(accountRepository, never()).save(sender);
        verify(accountRepository, never()).save(receiver);
    }

    @Test
    @DisplayName("6. Blocked transaction does not modify balances")
    void testBlockedTransactionDoesNotModifyBalances() {
        Transaction tx = new Transaction("TX-TEST01", sender, receiver, new BigDecimal("20000.00"),
                LocalDateTime.now(), Transaction.TransactionStatus.FLAGGED);
        tx.setId(10L);

        when(transactionRepository.findById(10L)).thenReturn(Optional.of(tx));
        when(reviewOutcomeRepository.existsByTransactionId(10L)).thenReturn(false);
        when(flaggedTransactionRepository.findByTransactionId(10L)).thenReturn(Collections.emptyList());
        when(reviewOutcomeRepository.save(any(ReviewOutcome.class))).thenAnswer(i -> i.getArgument(0));

        BigDecimal initialSenderBalance = sender.getBalance();
        BigDecimal initialReceiverBalance = receiver.getBalance();

        ReviewOutcome outcome = reviewService.blockTransaction(10L, "Reviewer", "Fraud confirmed");

        assertNotNull(outcome);
        assertEquals(ReviewOutcome.Outcome.BLOCKED, outcome.getOutcome());
        assertEquals(Transaction.TransactionStatus.BLOCKED, tx.getStatus());
        assertEquals(initialSenderBalance, sender.getBalance());
        assertEquals(initialReceiverBalance, receiver.getBalance());
        verify(accountRepository, never()).save(sender);
        verify(accountRepository, never()).save(receiver);
    }

    @Test
    @DisplayName("7. Approved transaction modifies balances exactly once")
    void testApprovedTransactionModifiesBalances() {
        Transaction tx = new Transaction("TX-TEST02", sender, receiver, new BigDecimal("20000.00"),
                LocalDateTime.now(), Transaction.TransactionStatus.FLAGGED);
        tx.setId(20L);

        when(transactionRepository.findById(20L)).thenReturn(Optional.of(tx));
        when(reviewOutcomeRepository.existsByTransactionId(20L)).thenReturn(false);
        when(flaggedTransactionRepository.findByTransactionId(20L)).thenReturn(Collections.emptyList());
        when(reviewOutcomeRepository.save(any(ReviewOutcome.class))).thenAnswer(i -> i.getArgument(0));

        ReviewOutcome outcome = reviewService.approveTransaction(20L, "Reviewer", "Verified transaction");

        assertNotNull(outcome);
        assertEquals(ReviewOutcome.Outcome.APPROVED, outcome.getOutcome());
        assertEquals(Transaction.TransactionStatus.APPROVED, tx.getStatus());
        assertEquals(new BigDecimal("80000.00"), sender.getBalance());
        assertEquals(new BigDecimal("70000.00"), receiver.getBalance());
        verify(accountRepository, times(1)).save(sender);
        verify(accountRepository, times(1)).save(receiver);
    }

    @Test
    @DisplayName("8. Insufficient balance is rejected")
    void testInsufficientBalanceRejected() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(sender));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(receiver));

        BigDecimal excessiveAmount = new BigDecimal("200000.00");

        assertThrows(InsufficientBalanceException.class, () ->
                transactionService.createTransaction(1L, 2L, excessiveAmount)
        );
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("9. Invalid amount (zero or negative) is rejected")
    void testInvalidAmountRejected() {
        assertThrows(BadRequestException.class, () ->
                transactionService.createTransaction(1L, 2L, BigDecimal.ZERO)
        );
        assertThrows(BadRequestException.class, () ->
                transactionService.createTransaction(1L, 2L, new BigDecimal("-100.00"))
        );
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("10. Already reviewed transaction cannot be reviewed again")
    void testAlreadyReviewedTransactionCannotBeReviewedAgain() {
        Transaction completedTx = new Transaction("TX-TEST03", sender, receiver, new BigDecimal("5000.00"),
                LocalDateTime.now(), Transaction.TransactionStatus.COMPLETED);
        completedTx.setId(30L);

        when(transactionRepository.findById(30L)).thenReturn(Optional.of(completedTx));

        // Non-FLAGGED transaction cannot be approved or blocked
        assertThrows(ConflictException.class, () ->
                reviewService.approveTransaction(30L, "Reviewer", "Try approve")
        );
        assertThrows(ConflictException.class, () ->
                reviewService.blockTransaction(30L, "Reviewer", "Try block")
        );

        // FLAGGED transaction already reviewed cannot be reviewed again
        Transaction flaggedTx = new Transaction("TX-TEST04", sender, receiver, new BigDecimal("5000.00"),
                LocalDateTime.now(), Transaction.TransactionStatus.FLAGGED);
        flaggedTx.setId(40L);

        when(transactionRepository.findById(40L)).thenReturn(Optional.of(flaggedTx));
        when(reviewOutcomeRepository.existsByTransactionId(40L)).thenReturn(true);

        assertThrows(ConflictException.class, () ->
                reviewService.approveTransaction(40L, "Reviewer", "Duplicate approve")
        );
        assertThrows(ConflictException.class, () ->
                reviewService.blockTransaction(40L, "Reviewer", "Duplicate block")
        );
    }
}
