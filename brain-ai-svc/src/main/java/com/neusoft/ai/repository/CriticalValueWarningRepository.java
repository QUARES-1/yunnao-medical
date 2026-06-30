package com.neusoft.ai.repository;

import com.neusoft.ai.entity.CriticalValueWarning;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface CriticalValueWarningRepository extends JpaRepository<CriticalValueWarning, Long> {
    List<CriticalValueWarning> findByPatientIdAndDetectedTimeAfter(Long patientId, LocalDateTime time);
    Page<CriticalValueWarning> findByStatusOrderByDetectedTimeDesc(String status, Pageable pageable);
    Page<CriticalValueWarning> findByOrderByDetectedTimeDesc(Pageable pageable);
}
