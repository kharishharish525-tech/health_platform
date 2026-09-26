package com.ruralhealth.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummary {
    private long totalPatients;
    private long totalDoctors;
    private long waitingQueueCount;
    private long emergencyWaitingCount;
    private long todaysConsultations;
    private BigDecimal todaysRevenue;
    private BigDecimal totalUnpaidAmount;
    private long lowStockProductCount;
}
