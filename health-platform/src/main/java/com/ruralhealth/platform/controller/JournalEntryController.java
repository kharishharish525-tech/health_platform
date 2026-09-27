package com.ruralhealth.platform.controller;

import com.ruralhealth.platform.entity.JournalEntry;
import com.ruralhealth.platform.entity.JournalLine;
import com.ruralhealth.platform.repository.JournalEntryRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/journal-entries")
public class JournalEntryController {

    private final JournalEntryRepository repository;

    public JournalEntryController(JournalEntryRepository repository) {
        this.repository = repository;
    }

    public record JournalLineView(String accountCode, String accountName, BigDecimal debit, BigDecimal credit) {}
    public record JournalEntryView(Long id, LocalDateTime entryDate, String description, String referenceType,
                                   Long referenceId, List<JournalLineView> lines) {}

    @GetMapping
    public List<JournalEntryView> getAll() {
        return repository.findAll().stream().map(this::toView).toList();
    }

    private JournalEntryView toView(JournalEntry entry) {
        List<JournalLineView> lines = entry.getLines().stream().map(this::toView).toList();
        return new JournalEntryView(entry.getJournalEntryId(), entry.getEntryDate(), entry.getDescription(),
                entry.getReferenceType(), entry.getReferenceId(), lines);
    }

    private JournalLineView toView(JournalLine line) {
        return new JournalLineView(line.getAccount().getAccountCode(), line.getAccount().getAccountName(),
                line.getDebitAmount(), line.getCreditAmount());
    }
}