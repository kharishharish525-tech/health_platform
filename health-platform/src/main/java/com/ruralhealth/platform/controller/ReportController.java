package com.ruralhealth.platform.controller;

import com.ruralhealth.platform.dto.BalanceSheetReport;
import com.ruralhealth.platform.dto.ProfitLossReport;
import com.ruralhealth.platform.service.ReportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    /** Stage 5: Profit & Loss statement derived from the journal ledger. */
    @GetMapping("/profit-loss")
    public ProfitLossReport profitAndLoss(@RequestParam(required = false) Integer year) {
        return reportService.profitAndLoss(year);
    }

    /** Stage 5: Balance Sheet derived from the journal ledger. */
    @GetMapping("/balance-sheet")
    public BalanceSheetReport balanceSheet() {
        return reportService.balanceSheet();
    }

    /** Downloadable CSV version of the P&L, for spreadsheets / printing / accountants. */
    @GetMapping("/profit-loss/csv")
    public ResponseEntity<String> profitAndLossCsv(@RequestParam(required = false) Integer year) {
        ProfitLossReport pl = reportService.profitAndLoss(year);
        StringBuilder csv = new StringBuilder("Account,Amount\nREVENUE\n");
        pl.getRevenueByAccount().forEach((name, amt) -> csv.append(escape(name)).append(',').append(amt).append('\n'));
        csv.append("Total Revenue,").append(pl.getTotalRevenue()).append("\n\nEXPENSES\n");
        pl.getExpensesByAccount().forEach((name, amt) -> csv.append(escape(name)).append(',').append(amt).append('\n'));
        csv.append("Total Expenses,").append(pl.getTotalExpenses()).append("\n\n");
        csv.append("Net Profit,").append(pl.getNetProfit()).append('\n');
        return csvResponse(csv.toString(), "profit_and_loss.csv");
    }

    /** Downloadable CSV version of the Balance Sheet. */
    @GetMapping("/balance-sheet/csv")
    public ResponseEntity<String> balanceSheetCsv() {
        BalanceSheetReport bs = reportService.balanceSheet();
        StringBuilder csv = new StringBuilder("Account,Amount\nASSETS\n");
        bs.getAssets().forEach((name, amt) -> csv.append(escape(name)).append(',').append(amt).append('\n'));
        csv.append("Total Assets,").append(bs.getTotalAssets()).append("\n\nLIABILITIES\n");
        bs.getLiabilities().forEach((name, amt) -> csv.append(escape(name)).append(',').append(amt).append('\n'));
        csv.append("Total Liabilities,").append(bs.getTotalLiabilities()).append("\n\nEQUITY\n");
        bs.getEquity().forEach((name, amt) -> csv.append(escape(name)).append(',').append(amt).append('\n'));
        csv.append("Total Equity,").append(bs.getTotalEquity()).append('\n');
        return csvResponse(csv.toString(), "balance_sheet.csv");
    }

    private String escape(String value) {
        return value == null ? "" : value.replace(",", " ");
    }

    private ResponseEntity<String> csvResponse(String csv, String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }
}
