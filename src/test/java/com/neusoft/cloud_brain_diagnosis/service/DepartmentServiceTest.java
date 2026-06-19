package com.neusoft.cloud_brain_diagnosis.service;

import com.neusoft.cloud_brain_diagnosis.common.exception.BusinessException;
import com.neusoft.cloud_brain_diagnosis.entity.Department;
import com.neusoft.cloud_brain_diagnosis.entity.Doctor;
import com.neusoft.cloud_brain_diagnosis.repository.DepartmentRepository;
import com.neusoft.cloud_brain_diagnosis.repository.DoctorRepository;
import com.neusoft.cloud_brain_diagnosis.service.impl.DepartmentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * DepartmentService 单元测试
 * 覆盖：列表、详情、增删改
 */
@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {

    @Mock private DepartmentRepository departmentRepository;
    @Mock private DoctorRepository doctorRepository;

    private DepartmentServiceImpl departmentService;

    @BeforeEach
    void setUp() {
        departmentService = new DepartmentServiceImpl(departmentRepository, doctorRepository);
    }

    @Test
    void getAllDepartments_ShouldReturnSortedList() {
        Department dept1 = new Department();
        dept1.setId(1L);
        dept1.setName("内科");
        dept1.setSort(1);

        Department dept2 = new Department();
        dept2.setId(2L);
        dept2.setName("外科");
        dept2.setSort(2);

        when(departmentRepository.findAllByOrderBySortAsc()).thenReturn(List.of(dept1, dept2));

        List<Department> list = departmentService.getAllDepartments();
        assertEquals(2, list.size());
        assertEquals("内科", list.get(0).getName());
    }

    @Test
    void getDetail_ShouldReturnDepartment() {
        Department dept = new Department();
        dept.setId(1L);
        dept.setName("儿科");

        when(departmentRepository.findById(1L)).thenReturn(Optional.of(dept));

        Department result = departmentService.getDetail(1L);
        assertEquals("儿科", result.getName());
    }

    @Test
    void getDetail_ShouldThrow_WhenNotFound() {
        when(departmentRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> departmentService.getDetail(99L));
    }

    @Test
    void addDepartment_ShouldSucceed() {
        Department dept = new Department();
        dept.setName("新科室");

        when(departmentRepository.save(any())).thenReturn(dept);

        String result = departmentService.addDepartment(dept);
        assertEquals("科室添加成功", result);
    }

    @Test
    void updateDepartment_ShouldUpdateNonNullFields() {
        Department existing = new Department();
        existing.setId(1L);
        existing.setName("旧名");
        existing.setDescription("旧描述");

        Department update = new Department();
        update.setId(1L);
        update.setName("新名");
        update.setDescription(null); // 不更新

        when(departmentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(departmentRepository.save(any())).thenReturn(existing);

        String result = departmentService.updateDepartment(update);
        assertEquals("科室修改成功", result);
        assertEquals("新名", existing.getName());
        assertEquals("旧描述", existing.getDescription()); // 保持不变
    }

    @Test
    void updateDepartment_ShouldThrow_WhenNotFound() {
        Department update = new Department();
        update.setId(99L);
        when(departmentRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> departmentService.updateDepartment(update));
    }

    @Test
    void deleteDepartment_ShouldSucceed_WhenNoDoctors() {
        Department dept = new Department();
        dept.setId(1L);

        when(departmentRepository.findById(1L)).thenReturn(Optional.of(dept));
        when(doctorRepository.findByDepartmentId(1L)).thenReturn(List.of());

        String result = departmentService.deleteDepartment(1L);
        assertEquals("科室删除成功", result);
    }

    @Test
    void deleteDepartment_ShouldThrow_WhenHasDoctors() {
        Department dept = new Department();
        dept.setId(1L);

        when(departmentRepository.findById(1L)).thenReturn(Optional.of(dept));
        when(doctorRepository.findByDepartmentId(1L)).thenReturn(List.of(new Doctor(), new Doctor()));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> departmentService.deleteDepartment(1L));
        assertTrue(ex.getMessage().contains("2 名医生"));
    }

    @Test
    void deleteDepartment_ShouldThrow_WhenNotFound() {
        when(departmentRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> departmentService.deleteDepartment(99L));
    }
}
