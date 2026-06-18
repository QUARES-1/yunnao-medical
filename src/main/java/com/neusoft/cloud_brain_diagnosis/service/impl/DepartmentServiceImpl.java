package com.neusoft.cloud_brain_diagnosis.service.impl;

import com.neusoft.cloud_brain_diagnosis.entity.Department;
import com.neusoft.cloud_brain_diagnosis.repository.DepartmentRepository;
import com.neusoft.cloud_brain_diagnosis.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl implements DepartmentService {
    private final DepartmentRepository departmentRepository;

    @Override
    public List<Department> getAllDepartments() {
        return departmentRepository.findAllByOrderBySortAsc();
    }

    @Override
    public Department getDetail(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("科室不存在"));
    }

    @Override
    public String addDepartment(Department department) {
        departmentRepository.save(department);
        return "科室添加成功";
    }

    @Override
    public String updateDepartment(Department department) {
        Department exist = departmentRepository.findById(department.getId())
                .orElseThrow(() -> new RuntimeException("科室不存在"));
        if (department.getName() != null) exist.setName(department.getName());
        if (department.getDescription() != null) exist.setDescription(department.getDescription());
        if (department.getSort() != null) exist.setSort(department.getSort());
        departmentRepository.save(exist);
        return "科室修改成功";
    }

    @Override
    public String deleteDepartment(Long id) {
        departmentRepository.deleteById(id);
        return "科室删除成功";
    }
}