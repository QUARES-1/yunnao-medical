package com.neusoft.ai.repository;

import com.neusoft.ai.entity.FollowUpPlan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FollowUpPlanRepository extends JpaRepository<FollowUpPlan, Long> {
    Page<FollowUpPlan> findByPatientIdOrderByCreateTimeDesc(Long patientId, Pageable pageable);
    Page<FollowUpPlan> findByDoctorIdOrderByCreateTimeDesc(Long doctorId, Pageable pageable);
}
