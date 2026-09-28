package com.fraudwatch.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

@Entity
@Table(name = "rules")
public class Rule {

    public static final String RULE_AMOUNT_THRESHOLD = "AMOUNT_THRESHOLD";
    public static final String RULE_VELOCITY = "VELOCITY";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Rule name cannot be blank")
    @Column(nullable = false, length = 100)
    private String name;

    @NotBlank(message = "Rule code cannot be blank")
    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(length = 255)
    private String description;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "amount_threshold", precision = 19, scale = 2)
    private BigDecimal amountThreshold;

    @Column(name = "transaction_count_threshold")
    private Integer transactionCountThreshold;

    @Column(name = "window_seconds")
    private Integer windowSeconds;

    public Rule() {}

    public Rule(String name, String code, String description, boolean enabled, BigDecimal amountThreshold, Integer transactionCountThreshold, Integer windowSeconds) {
        this.name = name;
        this.code = code;
        this.description = description;
        this.enabled = enabled;
        this.amountThreshold = amountThreshold;
        this.transactionCountThreshold = transactionCountThreshold;
        this.windowSeconds = windowSeconds;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public BigDecimal getAmountThreshold() {
        return amountThreshold;
    }

    public void setAmountThreshold(BigDecimal amountThreshold) {
        this.amountThreshold = amountThreshold;
    }

    public Integer getTransactionCountThreshold() {
        return transactionCountThreshold;
    }

    public void setTransactionCountThreshold(Integer transactionCountThreshold) {
        this.transactionCountThreshold = transactionCountThreshold;
    }

    public Integer getWindowSeconds() {
        return windowSeconds;
    }

    public void setWindowSeconds(Integer windowSeconds) {
        this.windowSeconds = windowSeconds;
    }
}
