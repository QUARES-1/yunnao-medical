package com.neusoft.cloud_brain_diagnosis.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "triage_record")
@Data
public class TriageRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long patientId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String chiefComplaint;

    @Column(length = 100)
    private String recommendDepartment;

    private Long recommendDepartmentId;

    @Column(length = 500)
    private String recommendDoctorIds;

    @Column(columnDefinition = "TEXT")
    private String aiAnalysis;

    private Integer confidence;

    @Column(columnDefinition = "TEXT")
    private String rawResponse;

    @Column(length = 20)
    private String status;

    @Column(updatable = false)
    private LocalDateTime createTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        if (status == null) status = "success";
    }
}
