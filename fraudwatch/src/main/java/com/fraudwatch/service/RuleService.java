package com.fraudwatch.service;

import com.fraudwatch.entity.Rule;
import com.fraudwatch.exception.BadRequestException;
import com.fraudwatch.exception.ConflictException;
import com.fraudwatch.exception.NotFoundException;
import com.fraudwatch.repository.RuleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RuleService {

    private final RuleRepository ruleRepository;

    public RuleService(RuleRepository ruleRepository) {
        this.ruleRepository = ruleRepository;
    }

    public List<Rule> getAllRules() {
        return ruleRepository.findAll();
    }

    public Rule getRuleById(Long id) {
        return ruleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Rule not found with ID: " + id));
    }

    @Transactional
    public Rule createRule(Rule rule) {
        if (rule.getName() == null || rule.getName().trim().isEmpty()) {
            throw new BadRequestException("Rule name cannot be blank");
        }
        if (rule.getCode() == null || rule.getCode().trim().isEmpty()) {
            throw new BadRequestException("Rule code cannot be blank");
        }
        String code = rule.getCode().trim().toUpperCase();
        if (ruleRepository.existsByCode(code)) {
            throw new ConflictException("Rule code already exists: " + code);
        }

        rule.setCode(code);
        rule.setName(rule.getName().trim());
        return ruleRepository.save(rule);
    }

    @Transactional
    public Rule updateRule(Long id, Rule updatedRule) {
        Rule existing = getRuleById(id);

        if (updatedRule.getName() != null && !updatedRule.getName().trim().isEmpty()) {
            existing.setName(updatedRule.getName().trim());
        }
        if (updatedRule.getDescription() != null) {
            existing.setDescription(updatedRule.getDescription().trim());
        }
        if (updatedRule.getAmountThreshold() != null) {
            existing.setAmountThreshold(updatedRule.getAmountThreshold());
        }
        if (updatedRule.getTransactionCountThreshold() != null) {
            existing.setTransactionCountThreshold(updatedRule.getTransactionCountThreshold());
        }
        if (updatedRule.getWindowSeconds() != null) {
            existing.setWindowSeconds(updatedRule.getWindowSeconds());
        }
        existing.setEnabled(updatedRule.isEnabled());

        return ruleRepository.save(existing);
    }

    @Transactional
    public Rule toggleRuleEnabled(Long id, boolean enabled) {
        Rule rule = getRuleById(id);
        rule.setEnabled(enabled);
        return ruleRepository.save(rule);
    }
}
