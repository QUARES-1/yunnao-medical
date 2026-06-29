package com.neusoft.cloud_brain_diagnosis.repository;

import com.neusoft.cloud_brain_diagnosis.entity.MedicationGuide;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MedicationGuideRepository extends JpaRepository<MedicationGuide, Long> {
    List<MedicationGuide> findAllByPrescriptionIdOrderByIdDesc(Long prescriptionId);
    void deleteByPrescriptionId(Long prescriptionId);
}
