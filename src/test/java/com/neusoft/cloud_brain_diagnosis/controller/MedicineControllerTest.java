package com.neusoft.cloud_brain_diagnosis.controller;

import com.neusoft.cloud_brain_diagnosis.common.enums.RoleEnum;
import com.neusoft.cloud_brain_diagnosis.common.exception.BusinessException;
import com.neusoft.cloud_brain_diagnosis.common.util.JwtUtil;
import com.neusoft.cloud_brain_diagnosis.entity.Medicine;
import com.neusoft.cloud_brain_diagnosis.entity.MedicineCategory;
import com.neusoft.cloud_brain_diagnosis.service.MedicineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MedicineController Web层测试
 * 覆盖：药品列表、详情、增删改、库存、分类
 */
@WebMvcTest(MedicineController.class)
class MedicineControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private MedicineService medicineService;
    @MockBean private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        when(jwtUtil.validateToken(anyString())).thenReturn(true);
        when(jwtUtil.getUserIdFromToken(anyString())).thenReturn(1L);
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.ADMIN.getCode());
    }

    // ========== 药品列表 ==========

    @Test
    void getMedicineList_ShouldReturnPage() throws Exception {
        Medicine medicine = new Medicine();
        medicine.setId(1L);
        medicine.setName("阿莫西林");

        when(medicineService.getMedicineList(isNull(), isNull(), eq(1), eq(10)))
                .thenReturn(new PageImpl<>(List.of(medicine)));

        mockMvc.perform(get("/api/medicine/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].name").value("阿莫西林"));
    }

    @Test
    void getMedicineList_ShouldFilterByKeyword() throws Exception {
        when(medicineService.getMedicineList(eq("阿莫"), isNull(), eq(1), eq(10)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/medicine/list")
                        .param("keyword", "阿莫"))
                .andExpect(status().isOk());
    }

    @Test
    void getMedicineList_ShouldFilterByCategoryId() throws Exception {
        Medicine medicine = new Medicine();
        medicine.setId(1L);
        medicine.setName("阿莫西林");
        medicine.setCategoryId(2L);

        when(medicineService.getMedicineList(isNull(), eq(2L), eq(1), eq(10)))
                .thenReturn(new PageImpl<>(List.of(medicine)));

        mockMvc.perform(get("/api/medicine/list")
                        .param("categoryId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].name").value("阿莫西林"));
    }

    @Test
    void getMedicineList_ShouldSupportPagination() throws Exception {
        when(medicineService.getMedicineList(isNull(), isNull(), eq(2), eq(5)))
                .thenReturn(new PageImpl<>(List.of(), org.springframework.data.domain.PageRequest.of(1, 5), 0));

        mockMvc.perform(get("/api/medicine/list")
                        .param("page", "2")
                        .param("size", "5"))
                .andExpect(status().isOk());
    }

    // ========== 药品详情 ==========

    @Test
    void getDetail_ShouldReturnMedicine() throws Exception {
        Medicine medicine = new Medicine();
        medicine.setId(1L);
        medicine.setName("阿莫西林");
        medicine.setPrice(BigDecimal.valueOf(15.5));
        medicine.setStock(100);

        when(medicineService.getDetail(1L)).thenReturn(medicine);

        mockMvc.perform(get("/api/medicine/detail/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("阿莫西林"));
    }

    @Test
    void getDetail_ShouldReturn500_WhenNotFound() throws Exception {
        when(medicineService.getDetail(99L))
                .thenThrow(new BusinessException("药品不存在"));

        mockMvc.perform(get("/api/medicine/detail/99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    // ========== 添加药品 ==========

    @Test
    void addMedicine_ShouldSucceed() throws Exception {
        when(medicineService.addMedicine(any())).thenReturn("药品添加成功");

        mockMvc.perform(post("/api/medicine/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"阿莫西林\",\"price\":15.5,\"stock\":100}")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("药品添加成功"));
    }

    @Test
    void addMedicine_ShouldReturn500_WhenNameIsNull() throws Exception {
        when(medicineService.addMedicine(any()))
                .thenThrow(new BusinessException("药品名称不能为空"));

        mockMvc.perform(post("/api/medicine/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":null,\"price\":15.5}")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    // ========== 修改药品 ==========

    @Test
    void updateMedicine_ShouldSucceed() throws Exception {
        when(medicineService.updateMedicine(any())).thenReturn("药品更新成功");

        mockMvc.perform(put("/api/medicine/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":1,\"name\":\"阿莫西林（改）\",\"price\":20.0}")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("药品更新成功"));
    }

    @Test
    void updateMedicine_ShouldReturn500_WhenNotFound() throws Exception {
        when(medicineService.updateMedicine(any()))
                .thenThrow(new BusinessException("药品不存在"));

        mockMvc.perform(put("/api/medicine/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":99,\"name\":\"新药\"}")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    // ========== 删除药品 ==========

    @Test
    void deleteMedicine_ShouldSucceed() throws Exception {
        when(medicineService.deleteMedicine(1L)).thenReturn("药品删除成功");

        mockMvc.perform(delete("/api/medicine/delete/1")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("药品删除成功"));
    }

    @Test
    void deleteMedicine_ShouldReturn500_WhenNotFound() throws Exception {
        when(medicineService.deleteMedicine(99L))
                .thenThrow(new BusinessException("药品不存在"));

        mockMvc.perform(delete("/api/medicine/delete/99")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    // ========== 调整库存 ==========

    @Test
    void updateStock_ShouldIncrease() throws Exception {
        when(medicineService.updateStock(1L, 50)).thenReturn("库存更新成功，当前库存：150");

        mockMvc.perform(put("/api/medicine/stock/1")
                        .param("quantity", "50")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("库存更新成功，当前库存：150"));
    }

    @Test
    void updateStock_ShouldDecrease() throws Exception {
        when(medicineService.updateStock(1L, -10)).thenReturn("库存更新成功，当前库存：90");

        mockMvc.perform(put("/api/medicine/stock/1")
                        .param("quantity", "-10")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("库存更新成功，当前库存：90"));
    }

    @Test
    void updateStock_ShouldReturn500_WhenInsufficientStock() throws Exception {
        when(medicineService.updateStock(1L, -200))
                .thenThrow(new BusinessException("库存不足"));

        mockMvc.perform(put("/api/medicine/stock/1")
                        .param("quantity", "-200")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    // ========== 药品分类 ==========

    @Test
    void getCategoryList_ShouldReturnList() throws Exception {
        MedicineCategory cat1 = new MedicineCategory();
        cat1.setId(1L);
        cat1.setName("抗生素");

        MedicineCategory cat2 = new MedicineCategory();
        cat2.setId(2L);
        cat2.setName("退烧药");

        when(medicineService.getCategoryList()).thenReturn(List.of(cat1, cat2));

        mockMvc.perform(get("/api/medicine/category/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].name").value("抗生素"));
    }
}
