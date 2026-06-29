package com.neusoft.cloud_brain_diagnosis.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "medical_record_ai_generate")
@Data
public class MedicalRecordAiGenerate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long medicalRecordId;

    @Column(nullable = false)
    private Long doctorId;

    private Long patientId;

    @Column(columnDefinition = "TEXT")
    private String inputText;

    @Column(length = 20)
    private String inputType;

    @Column(length = 500)
    private String generatedChiefComplaint;

    @Column(columnDefinition = "TEXT")
    private String generatedPresentIllness;

    @Column(columnDefinition = "TEXT")
    private String generatedPastHistory;

    @Column(columnDefinition = "TEXT")
    private String generatedPhysicalExamination;

    @Column(length = 500)
    private String generatedDiagnosis;

    @Column(columnDefinition = "TEXT")
    private String generatedTreatment;

    @Column(columnDefinition = "TEXT")
    private String rawResponse;

    @Column(updatable = false)
    private LocalDateTime generateTime;

    @PrePersist
    protected void onCreate() {
        generateTime = LocalDateTime.now();
    }
}
