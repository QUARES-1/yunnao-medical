package com.neusoft.ai.repository;

import com.neusoft.ai.entity.ExaminationAiReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExaminationAiReviewRepository extends JpaRepository<ExaminationAiReview, Long> {
    Page<ExaminationAiReview> findByOrderByCreateTimeDesc(Pageable pageable);
    Page<ExaminationAiReview> findByDoctorIdOrderByCreateTimeDesc(Long doctorId, Pageable pageable);
}
