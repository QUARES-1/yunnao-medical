package com.neusoft.ai.repository;

import com.neusoft.ai.entity.ExaminationAiInterpretation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExaminationAiInterpretationRepository extends JpaRepository<ExaminationAiInterpretation, Long> {
    Page<ExaminationAiInterpretation> findByPatientIdOrderByCreateTimeDesc(Long patientId, Pageable pageable);
}
