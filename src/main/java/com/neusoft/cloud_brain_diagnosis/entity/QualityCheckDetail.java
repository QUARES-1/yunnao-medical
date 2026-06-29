package com.neusoft.cloud_brain_diagnosis.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "quality_check_detail")
@Data
public class QualityCheckDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long recordId;

    private Long targetId;

    @Column(length = 20)
    private String targetType;

    private Long doctorId;

    @Column(length = 50)
    private String doctorName;

    private Integer score;

    @Column(columnDefinition = "TEXT")
    private String problems;

    @Column(columnDefinition = "TEXT")
    private String suggestions;

    @Column(length = 20)
    private String status;

    @Column(length = 500)
    private String rectifyRemark;

    @Column(updatable = false)
    private LocalDateTime createTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        if (status == null) status = "pending";
    }
}
