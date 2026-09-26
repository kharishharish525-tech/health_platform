package com.ruralhealth.platform.service;

import com.ruralhealth.platform.entity.VendorBill;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class VendorBillServiceTest {

    @Test
    void shouldCreateVendorBillWithBillNumberAndAmount() {
        VendorBill bill = new VendorBill();
        bill.setBillNumber("VB-1001");
        bill.setAmount(BigDecimal.valueOf(1200.00));
        bill.setStatus("OPEN");

        assertEquals("VB-1001", bill.getBillNumber());
        assertEquals(BigDecimal.valueOf(1200.00), bill.getAmount());
        assertEquals("OPEN", bill.getStatus());
    }
}
