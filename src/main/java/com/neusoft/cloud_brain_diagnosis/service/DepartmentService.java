package com.neusoft.cloud_brain_diagnosis.service;

import com.neusoft.cloud_brain_diagnosis.entity.Department;
import java.util.List;

public interface DepartmentService {
    List<Department> getAllDepartments();
    Department getDetail(Long id);
    String addDepartment(Department department);
    String updateDepartment(Department department);
    String deleteDepartment(Long id);
}