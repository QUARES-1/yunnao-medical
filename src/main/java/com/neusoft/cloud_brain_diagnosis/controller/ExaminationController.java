package com.neusoft.cloud_brain_diagnosis.controller;

import com.neusoft.cloud_brain_diagnosis.common.annotation.RequireLogin;
import com.neusoft.cloud_brain_diagnosis.common.context.UserContext;
import com.neusoft.cloud_brain_diagnosis.common.enums.RoleEnum;
import com.neusoft.cloud_brain_diagnosis.common.result.Result;
import com.neusoft.cloud_brain_diagnosis.entity.Examination;
import com.neusoft.cloud_brain_diagnosis.entity.ExaminationItem;
import com.neusoft.cloud_brain_diagnosis.service.ExaminationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/examination")
@RequiredArgsConstructor
@Tag(name = "检查检验管理", description = "检查申请、结果填写、报告查询")
public class ExaminationController {

    private final ExaminationService examinationService;

    /**
     * 医生-开立检查
     */
    @PostMapping("/create")
    @RequireLogin(RoleEnum.DOCTOR)
    @Operation(summary = "医生-开立检查", description = "医生给患者开检查检验申请")
    public Result<Examination> createExamination(@RequestBody Examination examination) {
        return Result.success(examinationService.createExamination(examination, UserContext.getUserId()));
    }

    @PutMapping("/cancel/{id}")
    @RequireLogin(RoleEnum.DOCTOR)
    public Result<String> cancelExamination(@PathVariable Long id) {
        return Result.success(examinationService.cancelExamination(id, UserContext.getUserId()));
    }

    @GetMapping("/registration/{regId}")
    @RequireLogin(RoleEnum.DOCTOR)
    public Result<List<Examination>> getByRegistrationId(@PathVariable Long regId) {
        return Result.success(examinationService.getByRegistrationId(regId, UserContext.getUserId()));
    }

    /**
     * 检查详情（需要登录，患者/医生/检验科均可查看）
     */
    @GetMapping("/detail/{id}")
    @RequireLogin
    @Operation(summary = "检查详情", description = "患者、医生、检验科都可以查看")
    public Result<Examination> getDetail(@PathVariable Long id) {
        return Result.success(examinationService.getDetail(id, UserContext.getUserId(), UserContext.getRole()));
    }

    /**
     * 患者-我的检查报告
     */
    @GetMapping("/patient/list")
    @RequireLogin(RoleEnum.PATIENT)
    @Operation(summary = "患者-我的检查报告", description = "分页查询")
    public Result<Page<Examination>> getPatientList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Long patientId = UserContext.getUserId();
        return Result.success(examinationService.getPatientList(patientId, page, size));
    }

    /**
     * 医生-我开的检查
     */
    @GetMapping("/doctor/list")
    @RequireLogin(RoleEnum.DOCTOR)
    @Operation(summary = "医生-我开的检查", description = "分页查询")
    public Result<Page<Examination>> getDoctorList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Long doctorId = UserContext.getUserId();
        return Result.success(examinationService.getDoctorList(doctorId, page, size));
    }

    /**
     * 检验科-待检查列表
     */
    @GetMapping("/lab/list")
    @RequireLogin(RoleEnum.LAB)
    @Operation(summary = "检验科-待检查列表", description = "分页查询所有待检查的项目")
    public Result<Page<Examination>> getLabList(
            @RequestParam(defaultValue = "待检查") String status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(examinationService.getLabList(status, page, size));
    }

    /**
     * 检验科-填写检查结果
     */
    @PutMapping("/update-result")
    @RequireLogin(RoleEnum.LAB)
    @Operation(summary = "检验科-填写检查结果", description = "提交检查结果，状态改为已完成")
    public Result<String> updateResult(@RequestBody java.util.Map<String, Object> body) {
        Long id = body.get("id") instanceof Number number ? number.longValue() : Long.valueOf(String.valueOf(body.get("id")));
        String result = body.get("result") == null ? null : String.valueOf(body.get("result"));
        String resultImages = body.get("resultImages") == null ? null : String.valueOf(body.get("resultImages"));
        return Result.success(examinationService.updateResult(id, result, resultImages));
    }

    /**
     * 检查项目列表
     */
    @GetMapping("/item/list")
    @Operation(summary = "检查项目列表", description = "公开接口，支持按类型筛选（检查/检验）")
    public Result<List<ExaminationItem>> getItemList(@RequestParam(required = false) String type) {
        return Result.success(examinationService.getItemList(type));
    }
}
