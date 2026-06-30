package com.neusoft.ai.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "medical_record_ai_generate")
public class MedicalRecordAiGenerate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long doctorId;
    private Long patientId;
    private String inputText;
    private String inputType;
    private String generatedChiefComplaint;
    private String generatedPresentIllness;
    private String generatedPastHistory;
    private String generatedPhysicalExamination;
    private String generatedDiagnosis;
    private String generatedTreatment;
    private String rawResponse;
    private LocalDateTime generateTime;

    @PrePersist
    protected void onCreate() {
        generateTime = LocalDateTime.now();
    }
}
