package com.neusoft.ai.repository;

import com.neusoft.ai.entity.FollowUpRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FollowUpRecordRepository extends JpaRepository<FollowUpRecord, Long> {
    Page<FollowUpRecord> findByPatientIdAndStatus(Long patientId, String status, Pageable pageable);
}
