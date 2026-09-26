package com.ruralhealth.platform.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "vendor_bills")
public class VendorBill {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long vendorBillId;

    @ManyToOne
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @Column(nullable = false, unique = true, length = 50)
    private String billNumber;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    private String status = "OPEN"; // OPEN, PAID, PARTIAL
    private LocalDateTime billDate = LocalDateTime.now();
    private LocalDateTime paidAt;
    private String notes;
}
