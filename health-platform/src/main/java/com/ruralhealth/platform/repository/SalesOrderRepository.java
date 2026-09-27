package com.ruralhealth.platform.repository;

import com.ruralhealth.platform.entity.SalesOrder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalesOrderRepository extends JpaRepository<SalesOrder, Long> {
    boolean existsByConsultation_ConsultationId(Long consultationId);
}