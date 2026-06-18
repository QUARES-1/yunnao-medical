package com.neusoft.cloud_brain_diagnosis.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "examination")
@Data
public class Examination {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long registrationId;

    private Long patientId;
    private String patientName;

    private Long doctorId;
    private String doctorName;

    private Long departmentId;

    private Long itemId;
    private String itemName;

    private String type;

    @Column(columnDefinition = "TEXT")
    private String result;

    @Column(columnDefinition = "TEXT")
    private String resultImages;

    private String status;

    @Column(updatable = false)
    private LocalDateTime createTime;

    private LocalDateTime completeTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        if (status == null) status = "待检查";
    }
}