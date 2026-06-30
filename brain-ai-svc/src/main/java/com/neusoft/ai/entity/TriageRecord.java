package com.neusoft.ai.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "triage_record")
public class TriageRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long patientId;
    private String chiefComplaint;
    private String recommendDepartment;
    private Long recommendDepartmentId;
    private String recommendDoctorIds;
    private String aiAnalysis;
    private Integer confidence;
    private String rawResponse;
    private String status;
    private LocalDateTime createTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
    }
}
