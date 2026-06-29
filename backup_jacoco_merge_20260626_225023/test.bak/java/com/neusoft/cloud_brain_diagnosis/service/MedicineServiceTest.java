package com.neusoft.cloud_brain_diagnosis.service;

import com.neusoft.cloud_brain_diagnosis.common.exception.BusinessException;
import com.neusoft.cloud_brain_diagnosis.entity.Medicine;
import com.neusoft.cloud_brain_diagnosis.entity.MedicineCategory;
import com.neusoft.cloud_brain_diagnosis.repository.MedicineCategoryRepository;
import com.neusoft.cloud_brain_diagnosis.repository.MedicineRepository;
import com.neusoft.cloud_brain_diagnosis.service.impl.MedicineServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * MedicineService 单元测试
 * 覆盖：药品增删改查、库存调整、分类管理
 */
@ExtendWith(MockitoExtension.class)
class MedicineServiceTest {

    @Mock private MedicineRepository medicineRepository;
    @Mock private MedicineCategoryRepository medicineCategoryRepository;

    private MedicineServiceImpl medicineService;

    @BeforeEach
    void setUp() {
        medicineService = new MedicineServiceImpl(medicineRepository, medicineCategoryRepository);
    }

    @Test
    void getMedicineList_ShouldReturnPage() {
        Page<Medicine> page = new PageImpl<>(List.of(new Medicine()));
        when(medicineRepository.findByKeywordAndCategory(any(), any(), any(Pageable.class)))
                .thenReturn(page);

        Page<Medicine> result = medicineService.getMedicineList(null, null, 1, 10);
        assertEquals(1, result.getContent().size());
    }

    @Test
    void getMedicineList_ShouldFilterByKeywordAndCategory() {
        Page<Medicine> page = new PageImpl<>(List.of(new Medicine()));
        when(medicineRepository.findByKeywordAndCategory(eq("阿莫西林"), eq(1L), any(Pageable.class)))
                .thenReturn(page);

        assertEquals(1, medicineService.getMedicineList("阿莫西林", 1L, 1, 10).getContent().size());
    }

    @Test
    void getDetail_ShouldReturnMedicine() {
        Medicine med = new Medicine();
        med.setId(1L);
        med.setName("阿莫西林");
        when(medicineRepository.findById(1L)).thenReturn(Optional.of(med));

        Medicine result = medicineService.getDetail(1L);
        assertEquals("阿莫西林", result.getName());
    }

    @Test
    void getDetail_ShouldThrow_WhenNotFound() {
        when(medicineRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> medicineService.getDetail(99L));
    }

    @Test
    void addMedicine_ShouldSucceed() {
        Medicine med = new Medicine();
        med.setName("新药品");

        when(medicineRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String result = medicineService.addMedicine(med);
        assertEquals("药品添加成功", result);
        assertEquals(Integer.valueOf(0), med.getStock()); // 默认库存
        assertEquals(BigDecimal.ZERO, med.getPrice());    // 默认价格
    }

    @Test
    void addMedicine_ShouldThrow_WhenNameEmpty() {
        Medicine med = new Medicine();
        med.setName(null);
        assertThrows(BusinessException.class, () -> medicineService.addMedicine(med));
    }

    @Test
    void addMedicine_ShouldThrow_WhenNameEmptyString() {
        Medicine med = new Medicine();
        med.setName("");
        assertThrows(BusinessException.class, () -> medicineService.addMedicine(med));
    }

    @Test
    void updateMedicine_ShouldUpdateNonNullFields() {
        Medicine existing = new Medicine();
        existing.setId(1L);
        existing.setName("旧名");
        existing.setStock(100);

        Medicine update = new Medicine();
        update.setId(1L);
        update.setName("新名");
        update.setStock(null); // 不更新

        when(medicineRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(medicineRepository.save(any())).thenReturn(existing);

        medicineService.updateMedicine(update);
        assertEquals("新名", existing.getName());
        assertEquals(Integer.valueOf(100), existing.getStock()); // 保持不变
    }

    @Test
    void updateMedicine_ShouldThrow_WhenNotFound() {
        Medicine update = new Medicine();
        update.setId(99L);
        when(medicineRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> medicineService.updateMedicine(update));
    }

    @Test
    void deleteMedicine_ShouldSucceed() {
        when(medicineRepository.existsById(1L)).thenReturn(true);
        String result = medicineService.deleteMedicine(1L);
        assertEquals("药品删除成功", result);
        verify(medicineRepository).deleteById(1L);
    }

    @Test
    void deleteMedicine_ShouldThrow_WhenNotFound() {
        when(medicineRepository.existsById(99L)).thenReturn(false);
        assertThrows(BusinessException.class, () -> medicineService.deleteMedicine(99L));
    }

    @Test
    void updateStock_ShouldIncrease() {
        Medicine med = new Medicine();
        med.setId(1L);
        med.setStock(50);

        when(medicineRepository.findById(1L)).thenReturn(Optional.of(med));
        when(medicineRepository.save(any())).thenReturn(med);

        String result = medicineService.updateStock(1L, 10);
        assertTrue(result.contains("60")); // 50 + 10
    }

    @Test
    void updateStock_ShouldDecrease() {
        Medicine med = new Medicine();
        med.setId(1L);
        med.setStock(50);

        when(medicineRepository.findById(1L)).thenReturn(Optional.of(med));
        when(medicineRepository.save(any())).thenReturn(med);

        String result = medicineService.updateStock(1L, -10);
        assertTrue(result.contains("40")); // 50 - 10
    }

    @Test
    void updateStock_ShouldThrow_WhenInsufficient() {
        Medicine med = new Medicine();
        med.setId(1L);
        med.setStock(5);

        when(medicineRepository.findById(1L)).thenReturn(Optional.of(med));
        assertThrows(BusinessException.class, () -> medicineService.updateStock(1L, -10));
    }

    @Test
    void getCategoryList_ShouldReturnSorted() {
        MedicineCategory cat1 = new MedicineCategory();
        cat1.setId(1L);
        cat1.setName("抗生素");
        cat1.setSort(1);

        MedicineCategory cat2 = new MedicineCategory();
        cat2.setId(2L);
        cat2.setName("中成药");
        cat2.setSort(2);

        when(medicineCategoryRepository.findAllByOrderBySortAsc()).thenReturn(List.of(cat1, cat2));

        List<MedicineCategory> list = medicineService.getCategoryList();
        assertEquals(2, list.size());
        assertEquals("抗生素", list.get(0).getName());
    }
}
