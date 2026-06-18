package com.neusoft.cloud_brain_diagnosis.service;

import com.neusoft.cloud_brain_diagnosis.entity.Prescription;
import org.springframework.data.domain.Page;

public interface PrescriptionService {
    /**
     * 医生-开具处方
     */
    Prescription createPrescription(Prescription prescription);

    /**
     * 处方详情
     */
    Prescription getDetail(Long id);

    /**
     * 患者-我的处方列表（分页）
     */
    Page<Prescription> getPatientList(Long patientId, Integer page, Integer size);

    /**
     * 医生-我开的处方列表（分页）
     */
    Page<Prescription> getDoctorList(Long doctorId, Integer page, Integer size);

    /**
     * 药房-待发药处方列表（分页）
     */
    Page<Prescription> getPharmacyList(Integer page, Integer size);

    /**
     * 药房-发药
     */
    String dispense(Long id);
}