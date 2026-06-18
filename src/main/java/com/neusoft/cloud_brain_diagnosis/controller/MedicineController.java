package com.neusoft.cloud_brain_diagnosis.controller;

import com.neusoft.cloud_brain_diagnosis.common.annotation.RequireLogin;
import com.neusoft.cloud_brain_diagnosis.common.enums.RoleEnum;
import com.neusoft.cloud_brain_diagnosis.common.result.Result;
import com.neusoft.cloud_brain_diagnosis.entity.Medicine;
import com.neusoft.cloud_brain_diagnosis.entity.MedicineCategory;
import com.neusoft.cloud_brain_diagnosis.service.MedicineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/medicine")
@RequiredArgsConstructor
@Tag(name = "药品管理", description = "药品增删改查、库存、分类")
public class MedicineController {

    private final MedicineService medicineService;

    /**
     * 药品列表（支持搜索、分类筛选）
     */
    @GetMapping("/list")
    @Operation(summary = "药品列表", description = "支持关键词搜索、分类筛选，分页")
    public Result<Page<Medicine>> getMedicineList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(medicineService.getMedicineList(keyword, categoryId, page, size));
    }

    /**
     * 药品详情
     */
    @GetMapping("/detail/{id}")
    @Operation(summary = "药品详情", description = "公开接口")
    public Result<Medicine> getDetail(@PathVariable Long id) {
        return Result.success(medicineService.getDetail(id));
    }

    /**
     * 管理员-添加药品
     */
    @PostMapping("/add")
    @RequireLogin(RoleEnum.ADMIN)
    @Operation(summary = "管理员-添加药品")
    public Result<String> addMedicine(@RequestBody Medicine medicine) {
        return Result.success(medicineService.addMedicine(medicine));
    }

    /**
     * 管理员-修改药品
     */
    @PutMapping("/update")
    @RequireLogin(RoleEnum.ADMIN)
    @Operation(summary = "管理员-修改药品")
    public Result<String> updateMedicine(@RequestBody Medicine medicine) {
        return Result.success(medicineService.updateMedicine(medicine));
    }

    /**
     * 管理员-删除药品
     */
    @DeleteMapping("/delete/{id}")
    @RequireLogin(RoleEnum.ADMIN)
    @Operation(summary = "管理员-删除药品")
    public Result<String> deleteMedicine(@PathVariable Long id) {
        return Result.success(medicineService.deleteMedicine(id));
    }

    /**
     * 调整库存（管理员和药房都可以）
     */
    @PutMapping("/stock/{id}")
    @RequireLogin({RoleEnum.ADMIN, RoleEnum.PHARMACY})
    @Operation(summary = "调整库存", description = "正数增加，负数减少")
    public Result<String> updateStock(
            @PathVariable Long id,
            @RequestParam Integer quantity) {
        return Result.success(medicineService.updateStock(id, quantity));
    }

    /**
     * 药品分类列表
     */
    @GetMapping("/category/list")
    @Operation(summary = "药品分类列表", description = "公开接口")
    public Result<List<MedicineCategory>> getCategoryList() {
        return Result.success(medicineService.getCategoryList());
    }
}