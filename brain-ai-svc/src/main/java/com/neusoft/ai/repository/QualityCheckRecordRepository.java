package com.neusoft.ai.repository;

import com.neusoft.ai.entity.QualityCheckRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QualityCheckRecordRepository extends JpaRepository<QualityCheckRecord, Long> {
    Page<QualityCheckRecord> findByOrderByCreateTimeDesc(Pageable pageable);
}
