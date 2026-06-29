package com.neusoft.cloud_brain_diagnosis.repository;

import com.neusoft.cloud_brain_diagnosis.entity.ExaminationAiInterpretation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ExaminationAiInterpretationRepository extends JpaRepository<ExaminationAiInterpretation, Long> {
    Optional<ExaminationAiInterpretation> findByExaminationId(Long examinationId);
}
