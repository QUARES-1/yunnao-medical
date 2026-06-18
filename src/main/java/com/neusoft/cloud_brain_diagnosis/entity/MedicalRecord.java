package com.neusoft.cloud_brain_diagnosis.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "medical_record")
@Data
public class MedicalRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long registrationId;

    private Long patientId;
    private String patientName;

    private Long doctorId;
    private String doctorName;

    private Long departmentId;

    @Column(length = 500)
    private String chiefComplaint;

    @Column(columnDefinition = "TEXT")
    private String presentIllness;

    @Column(columnDefinition = "TEXT")
    private String pastHistory;

    @Column(columnDefinition = "TEXT")
    private String physicalExamination;

    @Column(length = 500)
    private String diagnosis;

    @Column(columnDefinition = "TEXT")
    private String treatment;

    @Column(updatable = false)
    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        updateTime = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }
}