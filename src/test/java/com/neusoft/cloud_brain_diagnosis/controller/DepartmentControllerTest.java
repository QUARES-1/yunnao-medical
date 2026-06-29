package com.neusoft.cloud_brain_diagnosis.controller;

import com.neusoft.cloud_brain_diagnosis.common.enums.RoleEnum;
import com.neusoft.cloud_brain_diagnosis.common.exception.BusinessException;
import com.neusoft.cloud_brain_diagnosis.common.util.JwtUtil;
import com.neusoft.cloud_brain_diagnosis.entity.Department;
import com.neusoft.cloud_brain_diagnosis.service.DepartmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * DepartmentController Web层测试
 * 覆盖：科室列表、详情、增删改（管理员）
 */
@WebMvcTest(DepartmentController.class)
class DepartmentControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private DepartmentService departmentService;
    @MockBean private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        when(jwtUtil.validateToken(anyString())).thenReturn(true);
        when(jwtUtil.getUserIdFromToken(anyString())).thenReturn(1L);
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.ADMIN.getCode());
    }

    // ========== 公开接口 ==========

    @Test
    void getAllDepartments_ShouldReturnList() throws Exception {
        Department dept1 = new Department();
        dept1.setId(1L);
        dept1.setName("内科");
        dept1.setSort(1);

        Department dept2 = new Department();
        dept2.setId(2L);
        dept2.setName("外科");
        dept2.setSort(2);

        when(departmentService.getAllDepartments()).thenReturn(List.of(dept1, dept2));

        mockMvc.perform(get("/api/department/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].name").value("内科"))
                .andExpect(jsonPath("$.data[1].name").value("外科"));
    }

    @Test
    void getAllDepartments_ShouldReturnEmptyList() throws Exception {
        when(departmentService.getAllDepartments()).thenReturn(List.of());

        mockMvc.perform(get("/api/department/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void getDetail_ShouldReturnDepartment() throws Exception {
        Department dept = new Department();
        dept.setId(1L);
        dept.setName("内科");
        dept.setDescription("内科疾病诊疗");

        when(departmentService.getDetail(1L)).thenReturn(dept);

        mockMvc.perform(get("/api/department/detail/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("内科"));
    }

    @Test
    void getDetail_ShouldReturn500_WhenNotFound() throws Exception {
        when(departmentService.getDetail(99L))
                .thenThrow(new BusinessException("科室不存在"));

        mockMvc.perform(get("/api/department/detail/99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    // ========== 管理员接口 ==========

    @Test
    void addDepartment_ShouldSucceed() throws Exception {
        when(departmentService.addDepartment(any())).thenReturn("科室添加成功");

        mockMvc.perform(post("/api/department/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"骨科\",\"description\":\"骨科疾病\"}")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("科室添加成功"));
    }

    @Test
    void addDepartment_ShouldReturn500_WhenNameIsNull() throws Exception {
        when(departmentService.addDepartment(any()))
                .thenThrow(new BusinessException("科室名称不能为空"));

        mockMvc.perform(post("/api/department/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":null}")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void addDepartment_ShouldReturn500_WhenNameAlreadyExists() throws Exception {
        when(departmentService.addDepartment(any()))
                .thenThrow(new BusinessException("科室名称已存在"));

        mockMvc.perform(post("/api/department/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"内科\"}")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void updateDepartment_ShouldSucceed() throws Exception {
        when(departmentService.updateDepartment(any())).thenReturn("科室更新成功");

        mockMvc.perform(put("/api/department/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":1,\"name\":\"内科（改）\"}")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("科室更新成功"));
    }

    @Test
    void updateDepartment_ShouldReturn500_WhenNotFound() throws Exception {
        when(departmentService.updateDepartment(any()))
                .thenThrow(new BusinessException("科室不存在"));

        mockMvc.perform(put("/api/department/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":99,\"name\":\"骨科\"}")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void deleteDepartment_ShouldSucceed() throws Exception {
        when(departmentService.deleteDepartment(1L)).thenReturn("科室删除成功");

        mockMvc.perform(delete("/api/department/delete/1")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("科室删除成功"));
    }

    @Test
    void deleteDepartment_ShouldReturn500_WhenNotFound() throws Exception {
        when(departmentService.deleteDepartment(99L))
                .thenThrow(new BusinessException("科室不存在"));

        mockMvc.perform(delete("/api/department/delete/99")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void deleteDepartment_ShouldReturn500_WhenHasDoctors() throws Exception {
        when(departmentService.deleteDepartment(1L))
                .thenThrow(new BusinessException("该科室下有医生，无法删除"));

        mockMvc.perform(delete("/api/department/delete/1")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }
}
