package com.neusoft.cloud_brain_diagnosis.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "critical_value_warning")
@Data
public class CriticalValueWarning {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long examinationId;

    private Long patientId;

    @Column(length = 50)
    private String patientName;

    @Column(length = 11)
    private String patientPhone;

    private Long doctorId;

    @Column(length = 50)
    private String doctorName;

    @Column(columnDefinition = "TEXT")
    private String criticalItems;

    @Column(length = 20)
    private String warningLevel;

    @Column(length = 20)
    private String status;

    private LocalDateTime doctorConfirmTime;

    @Column(length = 500)
    private String doctorRemark;

    @Column(length = 500)
    private String labRemark;

    private Integer smsSent;

    @Column(updatable = false)
    private LocalDateTime createTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        if (status == null) status = "pending";
        if (smsSent == null) smsSent = 0;
    }
}
