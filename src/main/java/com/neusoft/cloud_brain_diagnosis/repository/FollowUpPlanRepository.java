package com.neusoft.cloud_brain_diagnosis.repository;

import com.neusoft.cloud_brain_diagnosis.entity.FollowUpPlan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FollowUpPlanRepository extends JpaRepository<FollowUpPlan, Long> {
    Page<FollowUpPlan> findByPatientIdOrderByCreateTimeDesc(Long patientId, Pageable pageable);
    Page<FollowUpPlan> findByDoctorIdOrderByCreateTimeDesc(Long doctorId, Pageable pageable);
    List<FollowUpPlan> findByPatientIdAndStatus(Long patientId, String status);
}
