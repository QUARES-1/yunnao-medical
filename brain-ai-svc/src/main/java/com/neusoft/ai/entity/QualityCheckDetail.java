package com.neusoft.ai.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "quality_check_detail")
public class QualityCheckDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long recordId;
    private Long targetId;
    private String targetType;
    private Long doctorId;
    private String doctorName;
    private Integer score;
    @Column(columnDefinition = "TEXT")
    private String problems;
    @Column(columnDefinition = "TEXT")
    private String suggestions;
    private String status;
    private String rectifyRemark;
    private LocalDateTime createTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
    }
}
