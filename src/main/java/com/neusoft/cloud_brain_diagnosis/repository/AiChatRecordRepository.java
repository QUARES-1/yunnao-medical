package com.neusoft.cloud_brain_diagnosis.repository;

import com.neusoft.cloud_brain_diagnosis.entity.AiChatRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiChatRecordRepository extends JpaRepository<AiChatRecord, Long> {
    Page<AiChatRecord> findByUserIdAndUserTypeOrderByCreateTimeDesc(Long userId, String userType, Pageable pageable);
    List<AiChatRecord> findBySessionIdOrderByCreateTimeAsc(String sessionId);
    Page<AiChatRecord> findByOrderByCreateTimeDesc(Pageable pageable);
}
