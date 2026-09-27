package com.ruralhealth.platform.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "sales_orders")
public class SalesOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long salesOrderId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @OneToOne(optional = false)
    @JoinColumn(name = "consultation_id", nullable = false, unique = true)
    @JsonIgnore
    private Consultation consultation;

    private LocalDateTime orderDate = LocalDateTime.now();
    private String status = "OPEN";
    private Long invoiceId;
}