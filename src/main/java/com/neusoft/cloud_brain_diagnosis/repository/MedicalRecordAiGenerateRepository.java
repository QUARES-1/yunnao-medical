package com.neusoft.cloud_brain_diagnosis.repository;

import com.neusoft.cloud_brain_diagnosis.entity.MedicalRecordAiGenerate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MedicalRecordAiGenerateRepository extends JpaRepository<MedicalRecordAiGenerate, Long> {
    Page<MedicalRecordAiGenerate> findByDoctorIdOrderByGenerateTimeDesc(Long doctorId, Pageable pageable);
    List<MedicalRecordAiGenerate> findByPatientIdOrderByGenerateTimeDesc(Long patientId);
}
