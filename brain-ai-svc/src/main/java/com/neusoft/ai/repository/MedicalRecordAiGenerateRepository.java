package com.neusoft.ai.repository;

import com.neusoft.ai.entity.MedicalRecordAiGenerate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicalRecordAiGenerateRepository extends JpaRepository<MedicalRecordAiGenerate, Long> {
    Page<MedicalRecordAiGenerate> findByDoctorIdOrderByGenerateTimeDesc(Long doctorId, Pageable pageable);
}
