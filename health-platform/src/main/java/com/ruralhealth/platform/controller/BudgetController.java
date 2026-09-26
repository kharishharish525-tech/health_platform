package com.ruralhealth.platform.controller;

import com.ruralhealth.platform.dto.BudgetUtilizationReport;
import com.ruralhealth.platform.entity.Budget;
import com.ruralhealth.platform.repository.BudgetRepository;
import com.ruralhealth.platform.service.BudgetService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/budgets")
public class BudgetController {

    private final BudgetRepository budgetRepository;
    private final BudgetService budgetService;

    public BudgetController(BudgetRepository budgetRepository, BudgetService budgetService) {
        this.budgetRepository = budgetRepository;
        this.budgetService = budgetService;
    }

    @GetMapping
    public List<Budget> getAll() {
        return budgetRepository.findAll();
    }

    /** Allocate a department's monthly budget. */
    @PostMapping
    public Budget allocate(@RequestBody Budget budget) {
        return budgetService.allocate(budget);
    }

    /** Record spend against a budget line, e.g. after a purchase order is received. */
    @PatchMapping("/{id}/spend")
    public Budget recordSpend(@PathVariable Long id, @RequestBody Map<String, BigDecimal> body) {
        return budgetService.recordSpend(id, body.get("amount"));
    }

    /** Stage 5: departmental budget utilization report for a given month. */
    @GetMapping("/utilization")
    public List<BudgetUtilizationReport> utilization(@RequestParam Integer year, @RequestParam Integer month) {
        return budgetService.utilizationReport(year, month);
    }

    /** Downloadable CSV version of the budget utilization report. */
    @GetMapping("/utilization/csv")
    public ResponseEntity<String> utilizationCsv(@RequestParam Integer year, @RequestParam Integer month) {
        List<BudgetUtilizationReport> rows = budgetService.utilizationReport(year, month);
        StringBuilder csv = new StringBuilder("Department,Fiscal Year,Fiscal Month,Allocated,Spent,Remaining,Utilization %\n");
        for (BudgetUtilizationReport r : rows) {
            csv.append(r.getDepartmentName().replace(",", " ")).append(',')
               .append(r.getFiscalYear()).append(',')
               .append(r.getFiscalMonth()).append(',')
               .append(r.getAllocatedAmount()).append(',')
               .append(r.getSpentAmount()).append(',')
               .append(r.getRemainingAmount()).append(',')
               .append(String.format("%.2f", r.getUtilizationPercent()))
               .append('\n');
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"budget_utilization.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv.toString());
    }
}
