package com.fraudwatch.controller;

import com.fraudwatch.entity.Rule;
import com.fraudwatch.service.RuleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rules")
public class RuleController {

    private final RuleService ruleService;

    public RuleController(RuleService ruleService) {
        this.ruleService = ruleService;
    }

    @PostMapping
    public ResponseEntity<Rule> createRule(@Valid @RequestBody Rule rule) {
        Rule created = ruleService.createRule(rule);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<Rule>> getAllRules() {
        return ResponseEntity.ok(ruleService.getAllRules());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Rule> getRuleById(@PathVariable Long id) {
        return ResponseEntity.ok(ruleService.getRuleById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Rule> updateRule(@PathVariable Long id, @RequestBody Rule rule) {
        Rule updated = ruleService.updateRule(id, rule);
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/{id}/enabled")
    public ResponseEntity<Rule> toggleRuleEnabled(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Boolean> body,
            @RequestParam(required = false) Boolean enabled) {
        boolean isEnabled = true;
        if (body != null && body.containsKey("enabled")) {
            isEnabled = Boolean.TRUE.equals(body.get("enabled"));
        } else if (enabled != null) {
            isEnabled = enabled;
        }
        Rule updated = ruleService.toggleRuleEnabled(id, isEnabled);
        return ResponseEntity.ok(updated);
    }
}
