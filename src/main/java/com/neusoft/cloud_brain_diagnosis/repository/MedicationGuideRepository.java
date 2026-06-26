package com.neusoft.cloud_brain_diagnosis.repository;

import com.neusoft.cloud_brain_diagnosis.entity.MedicationGuide;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MedicationGuideRepository extends JpaRepository<MedicationGuide, Long> {
    Optional<MedicationGuide> findByPrescriptionId(Long prescriptionId);
}
