package com.neusoft.cloud_brain_diagnosis.repository;

import com.neusoft.cloud_brain_diagnosis.entity.TriageRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TriageRecordRepository extends JpaRepository<TriageRecord, Long> {
    Page<TriageRecord> findByPatientIdOrderByCreateTimeDesc(Long patientId, Pageable pageable);
    List<TriageRecord> findByPatientId(Long patientId);
}
