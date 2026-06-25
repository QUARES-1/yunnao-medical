package com.neusoft.cloud_brain_diagnosis.service.impl;

import com.neusoft.cloud_brain_diagnosis.common.exception.BusinessException;
import com.neusoft.cloud_brain_diagnosis.entity.Department;
import com.neusoft.cloud_brain_diagnosis.entity.Doctor;
import com.neusoft.cloud_brain_diagnosis.repository.DepartmentRepository;
import com.neusoft.cloud_brain_diagnosis.repository.DoctorRepository;
import com.neusoft.cloud_brain_diagnosis.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl implements DepartmentService {
    private final DepartmentRepository departmentRepository;
    private final DoctorRepository doctorRepository;

    @Override
    public List<Department> getAllDepartments() {
        return departmentRepository.findAllByOrderBySortAsc();
    }

    @Override
    public Department getDetail(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new BusinessException("科室不存在"));
    }

    @Override
    @Transactional
    public String addDepartment(Department department) {
        departmentRepository.save(department);
        return "科室添加成功";
    }

    @Override
    @Transactional
    public String updateDepartment(Department department) {
        Department exist = departmentRepository.findById(department.getId())
                .orElseThrow(() -> new BusinessException("科室不存在"));
        if (department.getName() != null) exist.setName(department.getName());
        if (department.getDescription() != null) exist.setDescription(department.getDescription());
        if (department.getSort() != null) exist.setSort(department.getSort());
        departmentRepository.save(exist);
        return "科室修改成功";
    }

    @Override
    @Transactional
    public String deleteDepartment(Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new BusinessException("科室不存在"));

        // 检查是否有医生属于该科室
        List<Doctor> doctors = doctorRepository.findByDepartmentId(id);
        if (!doctors.isEmpty()) {
            throw new BusinessException("该科室下还有 " + doctors.size() + " 名医生，无法删除");
        }

        departmentRepository.deleteById(id);
        return "科室删除成功";
    }
}
