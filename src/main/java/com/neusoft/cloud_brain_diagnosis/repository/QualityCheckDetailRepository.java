package com.neusoft.cloud_brain_diagnosis.repository;

import com.neusoft.cloud_brain_diagnosis.entity.QualityCheckDetail;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QualityCheckDetailRepository extends JpaRepository<QualityCheckDetail, Long> {
    Page<QualityCheckDetail> findByRecordIdOrderByCreateTimeDesc(Long recordId, Pageable pageable);
    List<QualityCheckDetail> findByRecordId(Long recordId);
    Page<QualityCheckDetail> findByDoctorIdOrderByCreateTimeDesc(Long doctorId, Pageable pageable);
    List<QualityCheckDetail> findByDoctorIdAndStatus(Long doctorId, String status);
}
