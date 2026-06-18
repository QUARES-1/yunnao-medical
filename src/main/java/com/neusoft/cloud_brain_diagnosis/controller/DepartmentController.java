package com.neusoft.cloud_brain_diagnosis.controller;

import com.neusoft.cloud_brain_diagnosis.common.annotation.RequireLogin;
import com.neusoft.cloud_brain_diagnosis.common.enums.RoleEnum;
import com.neusoft.cloud_brain_diagnosis.common.result.Result;
import com.neusoft.cloud_brain_diagnosis.entity.Department;
import com.neusoft.cloud_brain_diagnosis.service.DepartmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 科室管理Controller
 * 公开接口：列表、详情
 * 管理员接口：增删改
 */
@RestController
@RequestMapping("/api/department")
@RequiredArgsConstructor
@Tag(name = "科室管理", description = "科室的增删改查")
public class DepartmentController {

    private final DepartmentService departmentService;

    /**
     * 所有科室列表（公开）
     * 按排序号升序排列
     */
    @GetMapping("/list")
    @Operation(summary = "科室列表", description = "公开接口，按排序号返回所有科室")
    public Result<List<Department>> getAllDepartments() {
        return Result.success(departmentService.getAllDepartments());
    }

    /**
     * 科室详情（公开）
     */
    @GetMapping("/detail/{id}")
    @Operation(summary = "科室详情", description = "公开接口，根据ID查询科室详情")
    public Result<Department> getDetail(@PathVariable Long id) {
        return Result.success(departmentService.getDetail(id));
    }

    /**
     * 添加科室（管理员）
     */
    @PostMapping("/add")
    @RequireLogin(RoleEnum.ADMIN)
    @Operation(summary = "添加科室", description = "管理员权限")
    public Result<String> addDepartment(@RequestBody Department department) {
        return Result.success(departmentService.addDepartment(department));
    }

    /**
     * 修改科室（管理员）
     */
    @PutMapping("/update")
    @RequireLogin(RoleEnum.ADMIN)
    @Operation(summary = "修改科室", description = "管理员权限")
    public Result<String> updateDepartment(@RequestBody Department department) {
        return Result.success(departmentService.updateDepartment(department));
    }

    /**
     * 删除科室（管理员）
     */
    @DeleteMapping("/delete/{id}")
    @RequireLogin(RoleEnum.ADMIN)
    @Operation(summary = "删除科室", description = "管理员权限")
    public Result<String> deleteDepartment(@PathVariable Long id) {
        return Result.success(departmentService.deleteDepartment(id));
    }
}