package com.fraudwatch.service;

import com.fraudwatch.entity.Account;
import com.fraudwatch.exception.BadRequestException;
import com.fraudwatch.exception.ConflictException;
import com.fraudwatch.exception.NotFoundException;
import com.fraudwatch.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Transactional
    public Account createAccount(Account account) {
        if (account.getAccountNumber() == null || account.getAccountNumber().trim().isEmpty()) {
            throw new BadRequestException("Account number cannot be blank");
        }
        if (account.getOwnerName() == null || account.getOwnerName().trim().isEmpty()) {
            throw new BadRequestException("Owner name cannot be blank");
        }
        if (account.getBalance() == null || account.getBalance().compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Balance cannot be negative");
        }
        if (accountRepository.existsByAccountNumber(account.getAccountNumber().trim())) {
            throw new ConflictException("Account number already exists: " + account.getAccountNumber());
        }

        account.setAccountNumber(account.getAccountNumber().trim());
        account.setOwnerName(account.getOwnerName().trim());
        if (account.getCurrency() == null || account.getCurrency().trim().isEmpty()) {
            account.setCurrency("INR");
        }
        account.setActive(true);

        return accountRepository.save(account);
    }

    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }

    public Account getAccountById(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Account not found with ID: " + id));
    }
}
