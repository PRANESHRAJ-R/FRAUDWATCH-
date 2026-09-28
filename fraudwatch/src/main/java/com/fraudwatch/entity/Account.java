package com.fraudwatch.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Account number cannot be blank")
    @Column(name = "account_number", nullable = false, unique = true, length = 50)
    private String accountNumber;

    @NotBlank(message = "Owner name cannot be blank")
    @Column(name = "owner_name", nullable = false, length = 100)
    private String ownerName;

    @NotNull(message = "Balance cannot be null")
    @DecimalMin(value = "0.00", message = "Balance must not be negative")
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Column(nullable = false, length = 10)
    private String currency = "INR";

    @Column(nullable = false)
    private boolean active = true;

    public Account() {}

    public Account(String accountNumber, String ownerName, BigDecimal balance, String currency, boolean active) {
        this.accountNumber = accountNumber;
        this.ownerName = ownerName;
        this.balance = balance;
        this.currency = currency != null ? currency : "INR";
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
