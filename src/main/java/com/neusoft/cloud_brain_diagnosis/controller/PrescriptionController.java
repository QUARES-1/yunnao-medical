package com.neusoft.cloud_brain_diagnosis.controller;

import com.neusoft.cloud_brain_diagnosis.common.annotation.RequireLogin;
import com.neusoft.cloud_brain_diagnosis.common.context.UserContext;
import com.neusoft.cloud_brain_diagnosis.common.enums.RoleEnum;
import com.neusoft.cloud_brain_diagnosis.common.result.Result;
import com.neusoft.cloud_brain_diagnosis.entity.Prescription;
import com.neusoft.cloud_brain_diagnosis.service.PrescriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/prescription")
@RequiredArgsConstructor
@Tag(name = "处方管理", description = "处方开具、查询、发药")
public class PrescriptionController {

    private final PrescriptionService prescriptionService;

    /**
     * 医生-开具处方
     */
    @PostMapping("/create")
    @RequireLogin(RoleEnum.DOCTOR)
    @Operation(summary = "医生-开具处方", description = "医生给患者开处方")
    public Result<Prescription> createPrescription(@RequestBody Prescription prescription) {
        return Result.success(prescriptionService.createPrescription(prescription, UserContext.getUserId()));
    }

    @PutMapping("/cancel/{id}")
    @RequireLogin(RoleEnum.DOCTOR)
    public Result<String> cancelPrescription(@PathVariable Long id) {
        return Result.success(prescriptionService.cancelPrescription(id, UserContext.getUserId()));
    }

    @GetMapping("/registration/{regId}")
    @RequireLogin(RoleEnum.DOCTOR)
    public Result<List<Prescription>> getByRegistrationId(@PathVariable Long regId) {
        return Result.success(prescriptionService.getByRegistrationId(regId, UserContext.getUserId()));
    }

    /**
     * 处方详情（需要登录）
     */
    @GetMapping("/detail/{id}")
    @RequireLogin
    @Operation(summary = "处方详情", description = "患者、医生、药房都可以查看")
    public Result<Prescription> getDetail(@PathVariable Long id) {
        return Result.success(prescriptionService.getDetail(id, UserContext.getUserId(), UserContext.getRole()));
    }

    /**
     * 患者-我的处方
     */
    @GetMapping("/patient/list")
    @RequireLogin(RoleEnum.PATIENT)
    @Operation(summary = "患者-我的处方列表", description = "分页查询")
    public Result<Page<Prescription>> getPatientList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Long patientId = UserContext.getUserId();
        return Result.success(prescriptionService.getPatientList(patientId, page, size));
    }

    /**
     * 医生-我开的处方
     */
    @GetMapping("/doctor/list")
    @RequireLogin(RoleEnum.DOCTOR)
    @Operation(summary = "医生-我开的处方列表", description = "分页查询")
    public Result<Page<Prescription>> getDoctorList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Long doctorId = UserContext.getUserId();
        return Result.success(prescriptionService.getDoctorList(doctorId, page, size));
    }

    /**
     * 药房-待发药处方列表
     */
    @GetMapping("/pharmacy/list")
    @RequireLogin(RoleEnum.PHARMACY)
    @Operation(summary = "药房-待发药处方列表", description = "分页查询所有待发药的处方")
    public Result<Page<Prescription>> getPharmacyList(
            @RequestParam(defaultValue = "待发药") String status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(prescriptionService.getPharmacyList(status, page, size));
    }

    /**
     * 药房-发药
     */
    @PutMapping("/dispense/{id}")
    @RequireLogin(RoleEnum.PHARMACY)
    @Operation(summary = "药房-发药", description = "处方状态改为已发药")
    public Result<String> dispense(@PathVariable Long id) {
        return Result.success(prescriptionService.dispense(id));
    }
}
