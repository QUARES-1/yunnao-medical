package com.neusoft.ai.repository;

import com.neusoft.ai.entity.OperationAiReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OperationAiReportRepository extends JpaRepository<OperationAiReport, Long> {
    Page<OperationAiReport> findByReportTypeOrderByCreateTimeDesc(String reportType, Pageable pageable);
    Page<OperationAiReport> findByOrderByCreateTimeDesc(Pageable pageable);
}
