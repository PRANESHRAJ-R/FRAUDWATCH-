package com.fraudwatch.service;

import com.fraudwatch.entity.Account;
import com.fraudwatch.entity.Rule;
import com.fraudwatch.repository.RuleRepository;
import com.fraudwatch.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class FraudDetectionService {

    public static class FraudCheckResult {
        private final Rule rule;
        private final String reason;

        public FraudCheckResult(Rule rule, String reason) {
            this.rule = rule;
            this.reason = reason;
        }

        public Rule getRule() {
            return rule;
        }

        public String getReason() {
            return reason;
        }
    }

    private final RuleRepository ruleRepository;
    private final TransactionRepository transactionRepository;

    public FraudDetectionService(RuleRepository ruleRepository, TransactionRepository transactionRepository) {
        this.ruleRepository = ruleRepository;
        this.transactionRepository = transactionRepository;
    }

    public List<FraudCheckResult> evaluate(Account sender, BigDecimal amount, LocalDateTime timestamp) {
        List<Rule> enabledRules = ruleRepository.findByEnabledTrue();
        List<FraudCheckResult> triggeredRules = new ArrayList<>();

        for (Rule rule : enabledRules) {
            if (Rule.RULE_AMOUNT_THRESHOLD.equalsIgnoreCase(rule.getCode())) {
                if (rule.getAmountThreshold() != null && amount.compareTo(rule.getAmountThreshold()) > 0) {
                    String reason = String.format("Transaction amount %.2f exceeds threshold %.2f",
                            amount, rule.getAmountThreshold());
                    triggeredRules.add(new FraudCheckResult(rule, reason));
                }
            } else if (Rule.RULE_VELOCITY.equalsIgnoreCase(rule.getCode())) {
                if (rule.getTransactionCountThreshold() != null && rule.getWindowSeconds() != null) {
                    LocalDateTime windowStart = timestamp.minusSeconds(rule.getWindowSeconds());
                    long recentCount = transactionRepository.countRecentTransactionsBySender(sender.getId(), windowStart);
                    if (recentCount >= rule.getTransactionCountThreshold()) {
                        String reason = String.format("Account %s exceeded velocity limit: %d transactions in the last %d seconds (threshold: %d)",
                                sender.getAccountNumber(), recentCount, rule.getWindowSeconds(), rule.getTransactionCountThreshold());
                        triggeredRules.add(new FraudCheckResult(rule, reason));
                    }
                }
            }
        }

        return triggeredRules;
    }
}
