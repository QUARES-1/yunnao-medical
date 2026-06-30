package com.neusoft.ai.repository;

import com.neusoft.ai.entity.PrescriptionAiReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrescriptionAiReviewRepository extends JpaRepository<PrescriptionAiReview, Long> {
    Page<PrescriptionAiReview> findByDoctorIdOrderByReviewTimeDesc(Long doctorId, Pageable pageable);
    Page<PrescriptionAiReview> findByOrderByReviewTimeDesc(Pageable pageable);
}
