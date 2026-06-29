package com.neusoft.cloud_brain_diagnosis.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "follow_up_plan")
@Data
public class FollowUpPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long registrationId;

    @Column(nullable = false)
    private Long patientId;

    private Long doctorId;

    @Column(length = 200)
    private String disease;

    @Column(length = 50)
    private String planType;

    private Integer totalTimes;

    private Integer completedTimes;

    @Column(length = 20)
    private String status;

    @Column(updatable = false)
    private LocalDateTime createTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        if (completedTimes == null) completedTimes = 0;
        if (status == null) status = "ongoing";
    }
}
