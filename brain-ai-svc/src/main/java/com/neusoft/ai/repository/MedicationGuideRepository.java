package com.neusoft.ai.repository;

import com.neusoft.ai.entity.MedicationGuide;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicationGuideRepository extends JpaRepository<MedicationGuide, Long> {
    MedicationGuide findByPrescriptionId(Long prescriptionId);
}
