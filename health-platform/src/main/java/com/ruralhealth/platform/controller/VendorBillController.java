package com.ruralhealth.platform.controller;

import com.ruralhealth.platform.entity.Supplier;
import com.ruralhealth.platform.entity.VendorBill;
import com.ruralhealth.platform.repository.SupplierRepository;
import com.ruralhealth.platform.repository.VendorBillRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/vendor-bills")
public class VendorBillController {

    private final VendorBillRepository vendorBillRepository;
    private final SupplierRepository supplierRepository;

    public VendorBillController(VendorBillRepository vendorBillRepository, SupplierRepository supplierRepository) {
        this.vendorBillRepository = vendorBillRepository;
        this.supplierRepository = supplierRepository;
    }

    public record CreateVendorBillRequest(Long supplierId, String billNumber, BigDecimal amount, String notes) {}

    @GetMapping
    public List<VendorBill> getAll() {
        return vendorBillRepository.findAll();
    }

    @PostMapping
    public VendorBill create(@RequestBody CreateVendorBillRequest req) {
        Supplier supplier = supplierRepository.findById(req.supplierId())
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found: " + req.supplierId()));

        VendorBill bill = new VendorBill();
        bill.setSupplier(supplier);
        bill.setBillNumber(req.billNumber());
        bill.setAmount(req.amount() == null ? BigDecimal.ZERO : req.amount());
        bill.setNotes(req.notes());
        bill.setStatus("OPEN");
        return vendorBillRepository.save(bill);
    }

    @PatchMapping("/{id}/pay")
    public ResponseEntity<VendorBill> markPaid(@PathVariable Long id) {
        return vendorBillRepository.findById(id).map(bill -> {
            bill.setStatus("PAID");
            return ResponseEntity.ok(vendorBillRepository.save(bill));
        }).orElse(ResponseEntity.notFound().build());
    }
}
