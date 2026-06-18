package com.neusoft.cloud_brain_diagnosis.service.impl;

import com.neusoft.cloud_brain_diagnosis.entity.Medicine;
import com.neusoft.cloud_brain_diagnosis.entity.MedicineCategory;
import com.neusoft.cloud_brain_diagnosis.repository.MedicineCategoryRepository;
import com.neusoft.cloud_brain_diagnosis.repository.MedicineRepository;
import com.neusoft.cloud_brain_diagnosis.service.MedicineService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MedicineServiceImpl implements MedicineService {

    private final MedicineRepository medicineRepository;
    private final MedicineCategoryRepository medicineCategoryRepository;

    /**
     * 药品列表（支持搜索、分类筛选，分页）
     */
    @Override
    public Page<Medicine> getMedicineList(String keyword, Long categoryId, Integer page, Integer size) {
        PageRequest pageRequest = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createTime"));
        return medicineRepository.findByKeywordAndCategory(keyword, categoryId, pageRequest);
    }

    /**
     * 药品详情
     */
    @Override
    public Medicine getDetail(Long id) {
        return medicineRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("药品不存在"));
    }

    /**
     * 添加药品
     */
    @Override
    public String addMedicine(Medicine medicine) {
        // 1. 验证药品名称
        if (medicine.getName() == null || medicine.getName().isEmpty()) {
            throw new RuntimeException("药品名称不能为空");
        }

        // 2. 设置默认值
        if (medicine.getStock() == null) {
            medicine.setStock(0);
        }
        if (medicine.getPrice() == null) {
            medicine.setPrice(BigDecimal.ZERO);
        }

        // 3. 保存
        medicineRepository.save(medicine);
        return "药品添加成功";
    }

    /**
     * 修改药品
     */
    @Override
    public String updateMedicine(Medicine medicine) {
        // 1. 查找药品
        Medicine exist = medicineRepository.findById(medicine.getId())
                .orElseThrow(() -> new RuntimeException("药品不存在"));

        // 2. 更新非空字段
        if (medicine.getName() != null) exist.setName(medicine.getName());
        if (medicine.getCategoryId() != null) exist.setCategoryId(medicine.getCategoryId());
        if (medicine.getCategoryName() != null) exist.setCategoryName(medicine.getCategoryName());
        if (medicine.getSpecification() != null) exist.setSpecification(medicine.getSpecification());
        if (medicine.getUnit() != null) exist.setUnit(medicine.getUnit());
        if (medicine.getPrice() != null) exist.setPrice(medicine.getPrice());
        if (medicine.getStock() != null) exist.setStock(medicine.getStock());
        if (medicine.getManufacturer() != null) exist.setManufacturer(medicine.getManufacturer());
        if (medicine.getDescription() != null) exist.setDescription(medicine.getDescription());

        // 3. 保存
        medicineRepository.save(exist);
        return "药品修改成功";
    }

    /**
     * 删除药品
     */
    @Override
    public String deleteMedicine(Long id) {
        if (!medicineRepository.existsById(id)) {
            throw new RuntimeException("药品不存在");
        }
        medicineRepository.deleteById(id);
        return "药品删除成功";
    }

    /**
     * 调整库存
     * - quantity > 0：增加库存
     * - quantity < 0：减少库存
     */
    @Override
    public String updateStock(Long id, Integer quantity) {
        // 1. 查找药品
        Medicine medicine = medicineRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("药品不存在"));

        // 2. 计算新库存
        int newStock = medicine.getStock() + quantity;
        if (newStock < 0) {
            throw new RuntimeException("库存不足，当前库存：" + medicine.getStock());
        }

        // 3. 更新
        medicine.setStock(newStock);
        medicineRepository.save(medicine);

        return "库存更新成功，当前库存：" + newStock;
    }

    /**
     * 药品分类列表
     */
    @Override
    public List<MedicineCategory> getCategoryList() {
        return medicineCategoryRepository.findAllByOrderBySortAsc();
    }
}