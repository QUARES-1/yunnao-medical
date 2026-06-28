package com.neusoft.cloud_brain_diagnosis.controller;

import com.neusoft.cloud_brain_diagnosis.common.annotation.RequireLogin;
import com.neusoft.cloud_brain_diagnosis.common.context.UserContext;
import com.neusoft.cloud_brain_diagnosis.common.enums.RoleEnum;
import com.neusoft.cloud_brain_diagnosis.common.result.Result;
import com.neusoft.cloud_brain_diagnosis.entity.MedicalRecord;
import com.neusoft.cloud_brain_diagnosis.service.MedicalRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/medical-record")
@RequiredArgsConstructor
@Tag(name = "电子病历", description = "病历书写、查询")
public class MedicalRecordController {

    private final MedicalRecordService medicalRecordService;

    /**
     * 医生-保存病历（新增或修改）
     */
    @PostMapping("/save")
    @RequireLogin(RoleEnum.DOCTOR)
    @Operation(summary = "医生-保存病历", description = "新增或修改病历，一个挂号对应一份病历")
    public Result<MedicalRecord> saveRecord(@RequestBody MedicalRecord record) {
        return Result.success(medicalRecordService.saveRecord(record, UserContext.getUserId()));
    }

    /**
     * 病历详情（需要登录，患者和医生可以查看）
     */
    @GetMapping("/detail/{id}")
    @RequireLogin
    @Operation(summary = "病历详情", description = "患者和医生都可以查看")
    public Result<MedicalRecord> getDetail(@PathVariable Long id) {
        return Result.success(medicalRecordService.getDetail(id, UserContext.getUserId(), UserContext.getRole()));
    }

    /**
     * 根据挂号ID查询病历（需要登录）
     */
    @GetMapping("/registration/{regId}")
    @RequireLogin
    @Operation(summary = "根据挂号ID查询病历", description = "一个挂号对应一份病历")
    public Result<MedicalRecord> getByRegistrationId(@PathVariable Long regId) {
        return Result.success(medicalRecordService.getByRegistrationId(regId, UserContext.getUserId(), UserContext.getRole()));
    }

    /**
     * 患者-我的病历列表
     */
    @GetMapping("/patient/list")
    @RequireLogin(RoleEnum.PATIENT)
    @Operation(summary = "患者-我的病历列表", description = "分页查询")
    public Result<Page<MedicalRecord>> getPatientList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Long patientId = UserContext.getUserId();
        return Result.success(medicalRecordService.getPatientList(patientId, page, size));
    }

    /**
     * 医生-我写的病历列表
     */
    @GetMapping("/doctor/list")
    @RequireLogin(RoleEnum.DOCTOR)
    @Operation(summary = "医生-我的病历列表", description = "分页查询我写过的所有病历")
    public Result<Page<MedicalRecord>> getDoctorList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Long doctorId = UserContext.getUserId();
        return Result.success(medicalRecordService.getDoctorList(doctorId, page, size));
    }
}
