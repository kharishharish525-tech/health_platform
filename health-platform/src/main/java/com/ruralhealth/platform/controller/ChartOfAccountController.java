package com.ruralhealth.platform.controller;

import com.ruralhealth.platform.entity.ChartOfAccount;
import com.ruralhealth.platform.repository.ChartOfAccountRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/chart-of-accounts")
public class ChartOfAccountController {

    private static final Set<String> ACCOUNT_TYPES = Set.of("ASSET", "LIABILITY", "EQUITY", "REVENUE", "EXPENSE");
    private final ChartOfAccountRepository repository;

    public ChartOfAccountController(ChartOfAccountRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<ChartOfAccount> getAll() {
        return repository.findAll();
    }

    @PostMapping
    public ChartOfAccount create(@RequestBody ChartOfAccount account) {
        validate(account);
        return repository.save(account);
    }

    @PutMapping("/{id}")
    public ChartOfAccount update(@PathVariable Long id, @RequestBody ChartOfAccount updated) {
        validate(updated);
        ChartOfAccount account = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + id));
        account.setAccountCode(updated.getAccountCode());
        account.setAccountName(updated.getAccountName());
        account.setAccountType(updated.getAccountType());
        return repository.save(account);
    }

    private void validate(ChartOfAccount account) {
        if (account.getAccountCode() == null || account.getAccountCode().isBlank()
                || account.getAccountName() == null || account.getAccountName().isBlank()) {
            throw new IllegalArgumentException("Account code and name are required");
        }
        if (account.getAccountType() == null || !ACCOUNT_TYPES.contains(account.getAccountType().toUpperCase())) {
            throw new IllegalArgumentException("Account type must be ASSET, LIABILITY, EQUITY, REVENUE, or EXPENSE");
        }
        account.setAccountType(account.getAccountType().toUpperCase());
    }
}