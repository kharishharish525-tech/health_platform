package com.ruralhealth.platform.controller;

import com.ruralhealth.platform.entity.Consultation;
import com.ruralhealth.platform.entity.Invoice;
import com.ruralhealth.platform.entity.SalesOrder;
import com.ruralhealth.platform.repository.ConsultationRepository;
import com.ruralhealth.platform.repository.SalesOrderRepository;
import com.ruralhealth.platform.service.BillingService;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sales-orders")
public class SalesOrderController {
    private final SalesOrderRepository salesOrderRepository;
    private final ConsultationRepository consultationRepository;
    private final BillingService billingService;

    public SalesOrderController(SalesOrderRepository salesOrderRepository,
                                ConsultationRepository consultationRepository,
                                BillingService billingService) {
        this.salesOrderRepository = salesOrderRepository;
        this.consultationRepository = consultationRepository;
        this.billingService = billingService;
    }

    public record CreateSalesOrderRequest(Long consultationId) {}
    public record SalesOrderLineRequest(Long productId, Integer quantity) {}
    public record CreateInvoiceRequest(List<SalesOrderLineRequest> lines) {}

    @GetMapping
    public List<SalesOrder> getAll() {
        return salesOrderRepository.findAll();
    }

    @PostMapping
    public SalesOrder create(@RequestBody CreateSalesOrderRequest request) {
        if (request.consultationId() == null) {
            throw new IllegalArgumentException("Consultation is required");
        }
        Consultation consultation = consultationRepository.findById(request.consultationId())
                .orElseThrow(() -> new IllegalArgumentException("Consultation not found: " + request.consultationId()));
        if (salesOrderRepository.existsByConsultation_ConsultationId(request.consultationId())) {
            throw new IllegalStateException("A sales order already exists for this consultation");
        }

        SalesOrder order = new SalesOrder();
        order.setConsultation(consultation);
        order.setPatient(consultation.getPatient());
        return salesOrderRepository.save(order);
    }

    @PostMapping("/{id}/invoice")
    @Transactional
    public ResponseEntity<Invoice> createInvoice(@PathVariable Long id,
                                                  @RequestBody CreateInvoiceRequest request) {
        SalesOrder order = salesOrderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sales order not found: " + id));
        if (!"OPEN".equals(order.getStatus())) {
            throw new IllegalStateException("Sales order #" + id + " has already been invoiced");
        }
        List<BillingService.InvoiceLineRequest> lines = request.lines() == null ? null : request.lines().stream()
                .map(line -> new BillingService.InvoiceLineRequest(line.productId(), line.quantity()))
                .toList();
        Invoice invoice = billingService.createInvoice(order.getPatient().getPatientId(),
                order.getConsultation().getConsultationId(), lines);
        order.setInvoiceId(invoice.getInvoiceId());
        order.setStatus("INVOICED");
        salesOrderRepository.save(order);
        return ResponseEntity.ok(invoice);
    }
}