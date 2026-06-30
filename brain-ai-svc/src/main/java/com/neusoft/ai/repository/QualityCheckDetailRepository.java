package com.neusoft.ai.repository;

import com.neusoft.ai.entity.QualityCheckDetail;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QualityCheckDetailRepository extends JpaRepository<QualityCheckDetail, Long> {
    Page<QualityCheckDetail> findByRecordIdOrderByCreateTimeDesc(Long recordId, Pageable pageable);
    Page<QualityCheckDetail> findByDoctorIdOrderByCreateTimeDesc(Long doctorId, Pageable pageable);
}
