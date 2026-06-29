package com.neusoft.cloud_brain_diagnosis.repository;

import com.neusoft.cloud_brain_diagnosis.entity.ExaminationAiReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ExaminationAiReviewRepository extends JpaRepository<ExaminationAiReview, Long> {
    Optional<ExaminationAiReview> findByExaminationId(Long examinationId);
    Page<ExaminationAiReview> findByReviewResultOrderByReviewTimeDesc(String reviewResult, Pageable pageable);
    Page<ExaminationAiReview> findByOrderByReviewTimeDesc(Pageable pageable);
}
