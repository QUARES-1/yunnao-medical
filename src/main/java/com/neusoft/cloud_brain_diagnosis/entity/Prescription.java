package com.neusoft.cloud_brain_diagnosis.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "prescription")
@Data
public class Prescription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long registrationId;

    private Long patientId;
    private String patientName;

    private Long doctorId;
    private String doctorName;

    private Long departmentId;

    @Column(columnDefinition = "TEXT")
    private String drugs;

    private BigDecimal totalAmount;

    private String status;

    @Column(updatable = false)
    private LocalDateTime createTime;

    private LocalDateTime dispenseTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        if (status == null) status = "待发药";
    }
}