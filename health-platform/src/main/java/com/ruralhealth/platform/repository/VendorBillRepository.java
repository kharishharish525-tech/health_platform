package com.ruralhealth.platform.repository;

import com.ruralhealth.platform.entity.VendorBill;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VendorBillRepository extends JpaRepository<VendorBill, Long> {
	boolean existsByPurchaseOrder_PurchaseOrderId(Long purchaseOrderId);
}
