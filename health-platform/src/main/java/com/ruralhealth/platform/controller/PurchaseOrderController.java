package com.ruralhealth.platform.controller;

import com.ruralhealth.platform.entity.*;
import com.ruralhealth.platform.repository.*;
import com.ruralhealth.platform.service.BudgetService;
import com.ruralhealth.platform.service.JournalService;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/purchase-orders")
public class PurchaseOrderController {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final JournalService journalService;
    private final DepartmentRepository departmentRepository;
    private final BudgetService budgetService;

    public PurchaseOrderController(PurchaseOrderRepository purchaseOrderRepository, SupplierRepository supplierRepository,
                                    ProductRepository productRepository, JournalService journalService,
                                    DepartmentRepository departmentRepository, BudgetService budgetService) {
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.supplierRepository = supplierRepository;
        this.productRepository = productRepository;
        this.journalService = journalService;
        this.departmentRepository = departmentRepository;
        this.budgetService = budgetService;
    }

    public record PurchaseLineDto(Long productId, Integer quantity, BigDecimal unitCost) {}
    public record CreatePurchaseOrderRequest(Long supplierId, Long departmentId, List<PurchaseLineDto> lines) {}
    public record SupplierPaymentRequest(String method) {}

    @GetMapping
    public List<PurchaseOrder> getAll() {
        return purchaseOrderRepository.findAll();
    }

    @PostMapping
    @Transactional
    public PurchaseOrder create(@RequestBody CreatePurchaseOrderRequest req) {
        Supplier supplier = supplierRepository.findById(req.supplierId())
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found: " + req.supplierId()));

        PurchaseOrder po = new PurchaseOrder();
        po.setSupplier(supplier);
        if (req.departmentId() != null) {
            Department department = departmentRepository.findById(req.departmentId())
                .orElseThrow(() -> new IllegalArgumentException("Department not found: " + req.departmentId()));
            po.setDepartment(department);
        }

        BigDecimal total = BigDecimal.ZERO;
        for (PurchaseLineDto line : req.lines()) {
            Product product = productRepository.findById(line.productId())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found: " + line.productId()));

            PurchaseItem item = new PurchaseItem();
            item.setPurchaseOrder(po);
            item.setProduct(product);
            item.setQuantity(line.quantity());
            item.setUnitCost(line.unitCost());
            BigDecimal lineTotal = line.unitCost().multiply(BigDecimal.valueOf(line.quantity()));
            item.setLineTotal(lineTotal);

            po.getItems().add(item);
            total = total.add(lineTotal);
        }
        po.setTotalAmount(total);
        return purchaseOrderRepository.save(po);
    }

    /**
     * Mark a PO as received: restocks inventory for every line item and posts
     * Debit Medical Supplies Expense / Credit Accounts Payable.
     */
    @PatchMapping("/{id}/receive")
    @Transactional
    public ResponseEntity<PurchaseOrder> receive(@PathVariable Long id) {
        return purchaseOrderRepository.findById(id).map(po -> {
            if ("RECEIVED".equals(po.getStatus())) {
                throw new IllegalStateException("Purchase order #" + id + " was already received");
            }
            po.setStatus("RECEIVED");

            for (PurchaseItem item : po.getItems()) {
                Product product = item.getProduct();
                int current = product.getStockQuantity() == null ? 0 : product.getStockQuantity();
                product.setStockQuantity(current + item.getQuantity());
                productRepository.save(product);
            }

            purchaseOrderRepository.save(po);
            journalService.postEntry(
                    "Goods received for PO #" + id,
                    "PURCHASE_ORDER", id,
                    "5000", "2000", po.getTotalAmount()
            );
            if (po.getDepartment() != null) {
                LocalDateTime receivedAt = LocalDateTime.now();
                budgetService.recordDepartmentSpend(po.getDepartment().getDepartmentId(),
                        receivedAt.getYear(), receivedAt.getMonthValue(), po.getTotalAmount());
            }
            return ResponseEntity.ok(po);
        }).orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/pay")
    @Transactional
    public ResponseEntity<PurchaseOrder> pay(@PathVariable Long id, @RequestBody SupplierPaymentRequest request) {
        return purchaseOrderRepository.findById(id).map(po -> {
            if (!"RECEIVED".equals(po.getStatus())) {
                throw new IllegalStateException("Only received purchase orders can be paid");
            }
            String method = request.method() == null ? "" : request.method().toUpperCase();
            String paymentAccount = switch (method) {
                case "CASH" -> "1000";
                case "BANK", "CARD", "UPI", "DIGITAL" -> "1010";
                default -> throw new IllegalArgumentException("Payment method must be CASH or BANK");
            };
            journalService.postEntry("Supplier payment for PO #" + id, "SUPPLIER_PAYMENT", id,
                    "2000", paymentAccount, po.getTotalAmount());
            po.setStatus("PAID");
            return ResponseEntity.ok(purchaseOrderRepository.save(po));
        }).orElse(ResponseEntity.notFound().build());
    }
}
