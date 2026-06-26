package com.neusoft.cloud_brain_diagnosis.repository;

import com.neusoft.cloud_brain_diagnosis.entity.PrescriptionAiReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PrescriptionAiReviewRepository extends JpaRepository<PrescriptionAiReview, Long> {
    Page<PrescriptionAiReview> findByDoctorIdOrderByReviewTimeDesc(Long doctorId, Pageable pageable);
    Page<PrescriptionAiReview> findByOrderByReviewTimeDesc(Pageable pageable);
    PrescriptionAiReview findByPrescriptionId(Long prescriptionId);
}
