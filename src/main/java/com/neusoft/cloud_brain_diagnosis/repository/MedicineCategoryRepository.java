package com.neusoft.cloud_brain_diagnosis.repository;

import com.neusoft.cloud_brain_diagnosis.entity.MedicineCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MedicineCategoryRepository extends JpaRepository<MedicineCategory, Long> {
    List<MedicineCategory> findAllByOrderBySortAsc();
}