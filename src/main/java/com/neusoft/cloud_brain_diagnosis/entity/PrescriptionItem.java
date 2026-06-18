package com.neusoft.cloud_brain_diagnosis.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "prescription_item")
@Data
public class PrescriptionItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long prescriptionId;

    private Long medicineId;

    private String medicineName;

    private String specification;

    private Integer quantity;

    private String unit;

    private String dosage;
}