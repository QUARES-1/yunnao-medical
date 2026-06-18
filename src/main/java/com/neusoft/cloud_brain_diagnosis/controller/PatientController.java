package com.neusoft.cloud_brain_diagnosis.controller;

import com.neusoft.cloud_brain_diagnosis.common.annotation.RequireLogin;
import com.neusoft.cloud_brain_diagnosis.common.context.UserContext;
import com.neusoft.cloud_brain_diagnosis.common.enums.RoleEnum;
import com.neusoft.cloud_brain_diagnosis.common.result.Result;
import com.neusoft.cloud_brain_diagnosis.entity.Patient;
import com.neusoft.cloud_brain_diagnosis.service.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 患者端Controller（微信小程序）
 * 微信登录、个人信息、修改信息、绑定手机号
 */
@RestController
@RequestMapping("/api/patient")
@RequiredArgsConstructor
@Tag(name = "患者端-微信小程序", description = "微信小程序患者专用接口")
public class PatientController {

    private final PatientService patientService;

    /**
     * 微信小程序登录
     * 传入code，返回token和患者信息
     * 首次登录自动创建患者账号
     */
    @PostMapping("/wx-login")
    @Operation(summary = "微信登录", description = "公开接口，传入微信code，返回token")
    public Result<Map<String, Object>> wxLogin(@RequestParam String code) {
        return Result.success(patientService.wxLogin(code));
    }

    /**
     * 获取当前登录患者信息
     */
    @GetMapping("/info")
    @RequireLogin(RoleEnum.PATIENT)
    @Operation(summary = "获取患者信息", description = "患者登录后获取自己的信息")
    public Result<Patient> getPatientInfo() {
        Long patientId = UserContext.getUserId();
        return Result.success(patientService.getPatientInfo(patientId));
    }

    /**
     * 修改患者信息
     */
    @PutMapping("/update")
    @RequireLogin(RoleEnum.PATIENT)
    @Operation(summary = "修改患者信息", description = "患者修改自己的信息")
    public Result<String> updatePatientInfo(@RequestBody Patient patient) {
        patient.setId(UserContext.getUserId());
        return Result.success(patientService.updatePatientInfo(patient));
    }

    /**
     * 绑定手机号
     */
    @PostMapping("/bind-phone")
    @RequireLogin(RoleEnum.PATIENT)
    @Operation(summary = "绑定手机号", description = "患者绑定手机号")
    public Result<String> bindPhone(@RequestParam String phone) {
        Long patientId = UserContext.getUserId();
        return Result.success(patientService.bindPhone(patientId, phone));
    }
}