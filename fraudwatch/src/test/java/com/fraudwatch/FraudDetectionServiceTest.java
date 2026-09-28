package com.fraudwatch;

import com.fraudwatch.entity.Account;
import com.fraudwatch.entity.Rule;
import com.fraudwatch.repository.RuleRepository;
import com.fraudwatch.repository.TransactionRepository;
import com.fraudwatch.service.FraudDetectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FraudDetectionServiceTest {

    @Mock
    private RuleRepository ruleRepository;

    @Mock
    private TransactionRepository transactionRepository;

    private FraudDetectionService fraudDetectionService;
    private Account sender;

    @BeforeEach
    void setUp() {
        fraudDetectionService = new FraudDetectionService(ruleRepository, transactionRepository);
        sender = new Account("ACC1001", "Sender", new BigDecimal("100000.00"), "INR", true);
        sender.setId(1L);
    }

    @Test
    @DisplayName("Amount threshold triggers when amount is strictly greater than threshold")
    void testAmountThresholdRule() {
        Rule rule = new Rule("Amount Rule", "AMOUNT_THRESHOLD", "Threshold rule", true,
                new BigDecimal("50000.00"), null, null);
        when(ruleRepository.findByEnabledTrue()).thenReturn(List.of(rule));

        // Amount <= threshold does not trigger
        List<FraudDetectionService.FraudCheckResult> under = fraudDetectionService.evaluate(
                sender, new BigDecimal("50000.00"), LocalDateTime.now());
        assertTrue(under.isEmpty());

        // Amount > threshold triggers
        List<FraudDetectionService.FraudCheckResult> over = fraudDetectionService.evaluate(
                sender, new BigDecimal("50000.01"), LocalDateTime.now());
        assertEquals(1, over.size());
        assertEquals("AMOUNT_THRESHOLD", over.get(0).getRule().getCode());
    }

    @Test
    @DisplayName("Velocity rule triggers when recent transactions meet or exceed threshold")
    void testVelocityRule() {
        Rule rule = new Rule("Velocity Rule", "VELOCITY", "Velocity rule", true,
                null, 3, 60);
        when(ruleRepository.findByEnabledTrue()).thenReturn(List.of(rule));
        when(transactionRepository.countRecentTransactionsBySender(eq(1L), any(LocalDateTime.class)))
                .thenReturn(3L);

        List<FraudDetectionService.FraudCheckResult> result = fraudDetectionService.evaluate(
                sender, new BigDecimal("100.00"), LocalDateTime.now());

        assertEquals(1, result.size());
        assertEquals("VELOCITY", result.get(0).getRule().getCode());
    }
}
