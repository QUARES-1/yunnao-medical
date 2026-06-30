package com.neusoft.ai.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "follow_up_plan")
public class FollowUpPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long patientId;
    private Long doctorId;
    private Long registrationId;
    private String disease;
    private String planType;
    private Integer totalTimes;
    private Integer completedTimes;
    private String status;
    private LocalDateTime createTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        if (completedTimes == null) completedTimes = 0;
    }
}
