package com.ruralhealth.platform.controller;

import com.ruralhealth.platform.dto.DashboardSummary;
import com.ruralhealth.platform.entity.Invoice;
import com.ruralhealth.platform.repository.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Aggregated stats for the website's home dashboard page. */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final ConsultationRepository consultationRepository;
    private final InvoiceRepository invoiceRepository;
    private final ProductRepository productRepository;

    public DashboardController(PatientRepository patientRepository, DoctorRepository doctorRepository,
                                ConsultationRepository consultationRepository, InvoiceRepository invoiceRepository,
                                ProductRepository productRepository) {
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.consultationRepository = consultationRepository;
        this.invoiceRepository = invoiceRepository;
        this.productRepository = productRepository;
    }

    @GetMapping("/summary")
    public DashboardSummary summary() {
        long totalPatients = patientRepository.count();
        long totalDoctors = doctorRepository.count();

        var waiting = consultationRepository.findByStatusOrderByTriageScoreDesc("WAITING");
        long waitingQueueCount = waiting.size();
        long emergencyWaitingCount = waiting.stream()
                .filter(c -> "EMERGENCY".equals(c.getTriagePriority()))
                .count();

        LocalDate today = LocalDate.now();
        long todaysConsultations = consultationRepository.findAll().stream()
                .filter(c -> c.getCreatedAt() != null && c.getCreatedAt().toLocalDate().isEqual(today))
                .count();

        BigDecimal todaysRevenue = invoiceRepository.findAll().stream()
                .filter(inv -> inv.getInvoiceDate() != null && inv.getInvoiceDate().toLocalDate().isEqual(today))
                .map(Invoice::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalUnpaid = invoiceRepository.findAll().stream()
                .filter(inv -> !"PAID".equals(inv.getStatus()))
                .map(Invoice::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long lowStockCount = productRepository.findAll().stream()
                .filter(p -> Boolean.FALSE.equals(p.getIsService()))
                .filter(p -> p.getStockQuantity() != null && p.getReorderLevel() != null
                        && p.getStockQuantity() <= p.getReorderLevel())
                .count();

        return new DashboardSummary(totalPatients, totalDoctors, waitingQueueCount, emergencyWaitingCount,
                todaysConsultations, todaysRevenue, totalUnpaid, lowStockCount);
    }
}
