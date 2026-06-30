package com.neusoft.ai.repository;

import com.neusoft.ai.entity.TriageRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TriageRecordRepository extends JpaRepository<TriageRecord, Long> {
    Page<TriageRecord> findByPatientIdOrderByCreateTimeDesc(Long patientId, Pageable pageable);
}
