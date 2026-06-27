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
 * DepartmentService 白盒单元测试
 * 覆盖：列表查询、详情查询、增删改、关联医生校验
 */
@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private DoctorRepository doctorRepository;

    private DepartmentServiceImpl departmentService;

    @BeforeEach
    void setUp() {
        departmentService = new DepartmentServiceImpl(departmentRepository, doctorRepository);
    }

    // ========== 列表查询 ==========

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

        List<Department> result = departmentService.getAllDepartments();

        assertEquals(2, result.size());
        assertEquals("内科", result.get(0).getName());
        assertEquals("外科", result.get(1).getName());
        verify(departmentRepository).findAllByOrderBySortAsc();
    }

    @Test
    void getAllDepartments_ShouldReturnEmptyList() {
        when(departmentRepository.findAllByOrderBySortAsc()).thenReturn(List.of());
        List<Department> result = departmentService.getAllDepartments();
        assertTrue(result.isEmpty());
    }

    // ========== 详情查询 ==========

    @Test
    void getDetail_ShouldReturnDepartment() {
        Department dept = new Department();
        dept.setId(1L);
        dept.setName("内科");
        dept.setDescription("内科疾病诊疗");

        when(departmentRepository.findById(1L)).thenReturn(Optional.of(dept));

        Department result = departmentService.getDetail(1L);
        assertEquals(1L, result.getId());
        assertEquals("内科", result.getName());
    }

    @Test
    void getDetail_ShouldThrow_WhenNotFound() {
        when(departmentRepository.findById(99L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> departmentService.getDetail(99L));
        assertEquals("科室不存在", ex.getMessage());
    }

    // ========== 添加科室 ==========

    @Test
    void addDepartment_ShouldSucceed() {
        Department dept = new Department();
        dept.setName("骨科");
        dept.setDescription("骨科疾病");
        dept.setSort(3);

        when(departmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String result = departmentService.addDepartment(dept);
        assertEquals("科室添加成功", result);
        verify(departmentRepository).save(dept);
    }

    // ========== 修改科室 ==========

    @Test
    void updateDepartment_ShouldUpdateName() {
        Department existing = new Department();
        existing.setId(1L);
        existing.setName("内科");
        existing.setDescription("旧描述");
        existing.setSort(1);

        Department update = new Department();
        update.setId(1L);
        update.setName("心内科");

        when(departmentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(departmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String result = departmentService.updateDepartment(update);
        assertEquals("科室修改成功", result);
        assertEquals("心内科", existing.getName());
        assertEquals("旧描述", existing.getDescription()); // 不变
    }

    @Test
    void updateDepartment_ShouldUpdateDescription() {
        Department existing = new Department();
        existing.setId(1L);
        existing.setName("内科");

        Department update = new Department();
        update.setId(1L);
        update.setDescription("新描述");

        when(departmentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(departmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        departmentService.updateDepartment(update);
        assertEquals("新描述", existing.getDescription());
    }

    @Test
    void updateDepartment_ShouldUpdateSort() {
        Department existing = new Department();
        existing.setId(1L);
        existing.setSort(1);

        Department update = new Department();
        update.setId(1L);
        update.setSort(5);

        when(departmentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(departmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        departmentService.updateDepartment(update);
        assertEquals(5, existing.getSort());
    }

    @Test
    void updateDepartment_ShouldNotUpdateNullFields() {
        Department existing = new Department();
        existing.setId(1L);
        existing.setName("内科");
        existing.setDescription("描述");
        existing.setSort(1);

        Department update = new Department();
        update.setId(1L);
        // 所有字段都为 null，不应覆盖

        when(departmentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(departmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        departmentService.updateDepartment(update);
        assertEquals("内科", existing.getName());
        assertEquals("描述", existing.getDescription());
        assertEquals(1, existing.getSort());
    }

    @Test
    void updateDepartment_ShouldThrow_WhenNotFound() {
        Department update = new Department();
        update.setId(99L);

        when(departmentRepository.findById(99L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> departmentService.updateDepartment(update));
        assertEquals("科室不存在", ex.getMessage());
    }

    // ========== 删除科室 ==========

    @Test
    void deleteDepartment_ShouldSucceed_WhenNoDoctors() {
        Department dept = new Department();
        dept.setId(1L);
        dept.setName("新科室");

        when(departmentRepository.findById(1L)).thenReturn(Optional.of(dept));
        when(doctorRepository.findByDepartmentId(1L)).thenReturn(List.of());

        String result = departmentService.deleteDepartment(1L);
        assertEquals("科室删除成功", result);
        verify(departmentRepository).deleteById(1L);
    }

    @Test
    void deleteDepartment_ShouldThrow_WhenHasDoctors() {
        Department dept = new Department();
        dept.setId(1L);

        Doctor doctor1 = new Doctor();
        doctor1.setId(10L);
        doctor1.setName("医生A");

        Doctor doctor2 = new Doctor();
        doctor2.setId(20L);
        doctor2.setName("医生B");

        when(departmentRepository.findById(1L)).thenReturn(Optional.of(dept));
        when(doctorRepository.findByDepartmentId(1L)).thenReturn(List.of(doctor1, doctor2));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> departmentService.deleteDepartment(1L));
        assertTrue(ex.getMessage().contains("该科室下还有 2 名医生，无法删除"));
    }

    @Test
    void deleteDepartment_ShouldThrow_WhenDepartmentNotFound() {
        when(departmentRepository.findById(99L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> departmentService.deleteDepartment(99L));
        assertEquals("科室不存在", ex.getMessage());
    }

    @Test
    void deleteDepartment_ShouldThrow_WhenHasOneDoctor() {
        Department dept = new Department();
        dept.setId(1L);

        Doctor doctor = new Doctor();
        doctor.setId(10L);

        when(departmentRepository.findById(1L)).thenReturn(Optional.of(dept));
        when(doctorRepository.findByDepartmentId(1L)).thenReturn(List.of(doctor));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> departmentService.deleteDepartment(1L));
        assertTrue(ex.getMessage().contains("该科室下还有 1 名医生，无法删除"));
    }
}
