package com.neusoft.cloud_brain_diagnosis.service;

import com.neusoft.cloud_brain_diagnosis.entity.Prescription;
import org.springframework.data.domain.Page;

import java.util.List;

public interface PrescriptionService {
    /**
     * 医生-开具处方
     */
    Prescription createPrescription(Prescription prescription, Long doctorId);

    String cancelPrescription(Long id, Long doctorId);

    List<Prescription> getByRegistrationId(Long registrationId, Long doctorId);

    /**
     * 处方详情
     */
    Prescription getDetail(Long id, Long userId, String role);

    /**
     * 患者-我的处方列表（分页）
     */
    Page<Prescription> getPatientList(Long patientId, Integer page, Integer size);

    /**
     * 医生-我开的处方列表（分页）
     */
    Page<Prescription> getDoctorList(Long doctorId, Integer page, Integer size);

    /**
     * 药房-处方列表（分页，可按状态筛选）
     */
    Page<Prescription> getPharmacyList(String status, Integer page, Integer size);

    /**
     * 药房-发药
     */
    String dispense(Long id);
}
