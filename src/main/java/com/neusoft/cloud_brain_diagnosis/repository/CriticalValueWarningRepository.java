package com.neusoft.cloud_brain_diagnosis.repository;

import com.neusoft.cloud_brain_diagnosis.entity.CriticalValueWarning;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CriticalValueWarningRepository extends JpaRepository<CriticalValueWarning, Long> {
    Page<CriticalValueWarning> findByStatusOrderByCreateTimeDesc(String status, Pageable pageable);
    Page<CriticalValueWarning> findByOrderByCreateTimeDesc(Pageable pageable);
    List<CriticalValueWarning> findByDoctorIdAndStatus(Long doctorId, String status);
    List<CriticalValueWarning> findByStatus(String status);
}
