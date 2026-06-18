package com.neusoft.cloud_brain_diagnosis.service;

import com.neusoft.cloud_brain_diagnosis.entity.Medicine;
import com.neusoft.cloud_brain_diagnosis.entity.MedicineCategory;
import org.springframework.data.domain.Page;

import java.util.List;

public interface MedicineService {
    /**
     * 药品列表（支持搜索、分类筛选，分页）
     */
    Page<Medicine> getMedicineList(String keyword, Long categoryId, Integer page, Integer size);

    /**
     * 药品详情
     */
    Medicine getDetail(Long id);

    /**
     * 添加药品
     */
    String addMedicine(Medicine medicine);

    /**
     * 修改药品
     */
    String updateMedicine(Medicine medicine);

    /**
     * 删除药品
     */
    String deleteMedicine(Long id);

    /**
     * 调整库存（正数增加，负数减少）
     */
    String updateStock(Long id, Integer quantity);

    /**
     * 药品分类列表
     */
    List<MedicineCategory> getCategoryList();
}