package com.ruralhealth.platform.controller;

import com.ruralhealth.platform.entity.Supplier;
import com.ruralhealth.platform.entity.VendorBill;
import com.ruralhealth.platform.entity.PurchaseOrder;
import com.ruralhealth.platform.repository.PurchaseOrderRepository;
import com.ruralhealth.platform.repository.SupplierRepository;
import com.ruralhealth.platform.repository.VendorBillRepository;
import com.ruralhealth.platform.service.JournalService;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/vendor-bills")
public class VendorBillController {

    private final VendorBillRepository vendorBillRepository;
    private final SupplierRepository supplierRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final JournalService journalService;

    public VendorBillController(VendorBillRepository vendorBillRepository, SupplierRepository supplierRepository,
                                PurchaseOrderRepository purchaseOrderRepository, JournalService journalService) {
        this.vendorBillRepository = vendorBillRepository;
        this.supplierRepository = supplierRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.journalService = journalService;
    }

    public record CreateVendorBillRequest(Long supplierId, Long purchaseOrderId, String billNumber,
                                          BigDecimal amount, String notes) {}
    public record VendorBillPaymentRequest(String method) {}

    @GetMapping
    public List<VendorBill> getAll() {
        return vendorBillRepository.findAll();
    }

    @PostMapping
    @Transactional
    public VendorBill create(@RequestBody CreateVendorBillRequest req) {
        if (req.billNumber() == null || req.billNumber().isBlank()) {
            throw new IllegalArgumentException("Bill number is required");
        }

        VendorBill bill = new VendorBill();
        if (req.purchaseOrderId() != null) {
            PurchaseOrder purchaseOrder = purchaseOrderRepository.findById(req.purchaseOrderId())
                    .orElseThrow(() -> new IllegalArgumentException("Purchase order not found: " + req.purchaseOrderId()));
            if (!"RECEIVED".equals(purchaseOrder.getStatus())) {
                throw new IllegalStateException("A vendor bill can only be created from a received purchase order");
            }
            if (vendorBillRepository.existsByPurchaseOrder_PurchaseOrderId(req.purchaseOrderId())) {
                throw new IllegalStateException("A vendor bill already exists for purchase order #" + req.purchaseOrderId());
            }
            if (req.amount() != null && req.amount().compareTo(purchaseOrder.getTotalAmount()) != 0) {
                throw new IllegalArgumentException("Bill amount must match the purchase order total");
            }
            bill.setPurchaseOrder(purchaseOrder);
            bill.setSupplier(purchaseOrder.getSupplier());
            bill.setAmount(purchaseOrder.getTotalAmount());
        } else {
            Supplier supplier = supplierRepository.findById(req.supplierId())
                    .orElseThrow(() -> new IllegalArgumentException("Supplier not found: " + req.supplierId()));
            if (req.amount() == null || req.amount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Bill amount must be greater than zero");
            }
            bill.setSupplier(supplier);
            bill.setAmount(req.amount());
        }
        bill.setBillNumber(req.billNumber());
        bill.setNotes(req.notes());
        bill.setStatus("OPEN");
        bill = vendorBillRepository.save(bill);

        if (bill.getPurchaseOrder() == null) {
            journalService.postEntry("Vendor bill " + bill.getBillNumber() + " from " + bill.getSupplier().getName(),
                    "VENDOR_BILL", bill.getVendorBillId(), "5000", "2000", bill.getAmount());
        }
        return bill;
    }

    @PatchMapping("/{id}/pay")
    @Transactional
    public ResponseEntity<VendorBill> markPaid(@PathVariable Long id,
                                                @RequestBody(required = false) VendorBillPaymentRequest request) {
        return vendorBillRepository.findById(id).map(bill -> {
            if ("PAID".equals(bill.getStatus())) {
                throw new IllegalStateException("Vendor bill #" + id + " is already paid");
            }
            String method = request == null || request.method() == null ? "BANK" : request.method().toUpperCase();
            String paymentAccount = switch (method) {
                case "CASH" -> "1000";
                case "BANK", "CARD", "UPI", "DIGITAL" -> "1010";
                default -> throw new IllegalArgumentException("Payment method must be CASH or BANK");
            };
            journalService.postEntry("Payment for vendor bill " + bill.getBillNumber(),
                    "VENDOR_BILL_PAYMENT", bill.getVendorBillId(), "2000", paymentAccount, bill.getAmount());
            bill.setStatus("PAID");
            bill.setPaidAt(LocalDateTime.now());
            if (bill.getPurchaseOrder() != null) {
                bill.getPurchaseOrder().setStatus("PAID");
                purchaseOrderRepository.save(bill.getPurchaseOrder());
            }
            return ResponseEntity.ok(vendorBillRepository.save(bill));
        }).orElse(ResponseEntity.notFound().build());
    }
}
