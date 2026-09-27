package com.ruralhealth.platform.service;

import com.ruralhealth.platform.entity.*;
import com.ruralhealth.platform.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class BillingService {

    private final InvoiceRepository invoiceRepository;
    private final PatientRepository patientRepository;
    private final ConsultationRepository consultationRepository;
    private final ProductRepository productRepository;
    private final PaymentRepository paymentRepository;
    private final JournalService journalService;

    public BillingService(InvoiceRepository invoiceRepository, PatientRepository patientRepository,
                           ConsultationRepository consultationRepository, ProductRepository productRepository,
                           PaymentRepository paymentRepository, JournalService journalService) {
        this.invoiceRepository = invoiceRepository;
        this.patientRepository = patientRepository;
        this.consultationRepository = consultationRepository;
        this.productRepository = productRepository;
        this.paymentRepository = paymentRepository;
        this.journalService = journalService;
    }

    public record InvoiceLineRequest(Long productId, Integer quantity) {}

    @Transactional
    public Invoice createInvoice(Long patientId, Long consultationId, List<InvoiceLineRequest> lines) {
        if (patientId == null) {
            throw new IllegalArgumentException("Patient is required");
        }
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("At least one invoice line is required");
        }
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Patient not found: " + patientId));

        Invoice invoice = new Invoice();
        invoice.setPatient(patient);

        if (consultationId != null) {
            Consultation consultation = consultationRepository.findById(consultationId)
                    .orElseThrow(() -> new IllegalArgumentException("Consultation not found: " + consultationId));
            if (!consultation.getPatient().getPatientId().equals(patientId)) {
                throw new IllegalArgumentException("Consultation does not belong to the selected patient");
            }
            invoice.setConsultation(consultation);
        }

        BigDecimal total = BigDecimal.ZERO;
        for (InvoiceLineRequest line : lines) {
            if (line == null || line.productId() == null || line.quantity() == null || line.quantity() <= 0) {
                throw new IllegalArgumentException("Each invoice line needs a product and a positive quantity");
            }
            Product product = productRepository.findById(line.productId())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found: " + line.productId()));

            // Physical goods (isService = false) are deducted from inventory; services have unlimited "stock".
            if (Boolean.FALSE.equals(product.getIsService())) {
                int available = product.getStockQuantity() == null ? 0 : product.getStockQuantity();
                if (available < line.quantity()) {
                    throw new IllegalStateException("Insufficient stock for '" + product.getName()
                            + "': requested " + line.quantity() + ", only " + available + " in stock");
                }
                product.setStockQuantity(available - line.quantity());
                productRepository.save(product);
            }

            InvoiceItem item = new InvoiceItem();
            item.setInvoice(invoice);
            item.setProduct(product);
            item.setQuantity(line.quantity());
            item.setUnitPrice(product.getUnitPrice());
            BigDecimal lineTotal = product.getUnitPrice().multiply(BigDecimal.valueOf(line.quantity()));
            item.setLineTotal(lineTotal);

            invoice.getItems().add(item);
            total = total.add(lineTotal);
        }

        invoice.setTotalAmount(total);
        invoice.setStatus("UNPAID");
        invoice = invoiceRepository.save(invoice);

        // Post revenue recognition: Debit Accounts Receivable, Credit Diagnostic/Consultation Revenue
        journalService.postEntry(
                "Invoice #" + invoice.getInvoiceId() + " issued to patient " + patient.getFullName(),
                "INVOICE", invoice.getInvoiceId(),
                "1100", "4100", total
        );

        return invoice;
    }

    @SuppressWarnings("null")
    @Transactional
    public Payment recordPayment(Long invoiceId, BigDecimal amount, String method) {
        if (invoiceId == null) {
            throw new IllegalArgumentException("Invoice is required");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero");
        }
        if (method == null || !List.of("CASH", "BANK", "CARD", "UPI", "DIGITAL", "INSURANCE")
                .contains(method.toUpperCase())) {
            throw new IllegalArgumentException("Unsupported payment method");
        }
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + invoiceId));

        BigDecimal paidBefore = paymentRepository.findByInvoice_InvoiceId(invoiceId).stream()
                .map(Payment::getAmountPaid)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (paidBefore.add(amount).compareTo(invoice.getTotalAmount()) > 0) {
            throw new IllegalArgumentException("Payment exceeds the invoice balance");
        }

        Payment payment = new Payment();
        payment.setInvoice(invoice);
        payment.setAmountPaid(amount);
        payment.setPaymentMethod(method);
        payment = paymentRepository.save(payment);

        BigDecimal totalPaid = paymentRepository.findByInvoice_InvoiceId(invoiceId).stream()
                .map(existingPayment -> existingPayment.getAmountPaid())
                .reduce(BigDecimal.ZERO, (total, paymentAmount) -> total.add(paymentAmount));

        if (totalPaid.compareTo(invoice.getTotalAmount()) >= 0) {
            invoice.setStatus("PAID");
        } else if (totalPaid.compareTo(BigDecimal.ZERO) > 0) {
            invoice.setStatus("PARTIAL");
        }
        invoiceRepository.save(invoice);

        // Post cash receipt: Debit Cash, Credit Accounts Receivable
        journalService.postEntry(
                "Payment received for invoice #" + invoiceId,
                "PAYMENT", payment.getPaymentId(),
            isBankPayment(method) ? "1010" : "1000", "1100", amount
        );

        return payment;
    }

    private boolean isBankPayment(String method) {
        return method != null && List.of("BANK", "CARD", "UPI", "DIGITAL").contains(method.toUpperCase());
    }
}
