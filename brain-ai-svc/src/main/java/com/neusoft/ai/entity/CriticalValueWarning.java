package com.neusoft.ai.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "critical_value_warning")
public class CriticalValueWarning {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long examinationId;
    private Long patientId;
    private Long doctorId;
    private String itemName;
    private String itemValue;
    private String referenceRange;
    private String alertLevel;
    private String status;
    private String note;
    private LocalDateTime detectedTime;
    private LocalDateTime confirmTime;
    private LocalDateTime processTime;
}
