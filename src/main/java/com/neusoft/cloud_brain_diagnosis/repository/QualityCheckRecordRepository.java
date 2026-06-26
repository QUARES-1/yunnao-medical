package com.neusoft.cloud_brain_diagnosis.repository;

import com.neusoft.cloud_brain_diagnosis.entity.QualityCheckRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QualityCheckRecordRepository extends JpaRepository<QualityCheckRecord, Long> {
    Page<QualityCheckRecord> findByOrderByCreateTimeDesc(Pageable pageable);
    Page<QualityCheckRecord> findByCheckTypeOrderByCreateTimeDesc(String checkType, Pageable pageable);
}
