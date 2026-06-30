package com.neusoft.ai.repository;

import com.neusoft.ai.entity.AiChatRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiChatRecordRepository extends JpaRepository<AiChatRecord, Long> {
    Page<AiChatRecord> findByUserIdAndUserTypeOrderByCreateTimeDesc(Long userId, String userType, Pageable pageable);
    Page<AiChatRecord> findByOrderByCreateTimeDesc(Pageable pageable);
}
