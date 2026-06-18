package com.neusoft.cloud_brain_diagnosis.repository;

import com.neusoft.cloud_brain_diagnosis.entity.ExaminationItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ExaminationItemRepository extends JpaRepository<ExaminationItem, Long> {
    List<ExaminationItem> findByType(String type);
}