package com.neusoft.cloud_brain_diagnosis.repository;

import com.neusoft.cloud_brain_diagnosis.entity.FollowUpRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FollowUpRecordRepository extends JpaRepository<FollowUpRecord, Long> {
    Page<FollowUpRecord> findByPlanIdOrderByCreateTimeDesc(Long planId, Pageable pageable);
    Page<FollowUpRecord> findByPatientIdOrderByCreateTimeDesc(Long patientId, Pageable pageable);
    Page<FollowUpRecord> findByPatientIdAndStatus(Long patientId, String status, Pageable pageable);
    Page<FollowUpRecord> findByStatusOrderByCreateTimeDesc(String status, Pageable pageable);
    List<FollowUpRecord> findByPlanIdAndStatus(Long planId, String status);
}
