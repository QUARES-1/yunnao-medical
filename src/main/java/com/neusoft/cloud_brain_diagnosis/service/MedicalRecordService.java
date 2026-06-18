package com.neusoft.cloud_brain_diagnosis.service;

import com.neusoft.cloud_brain_diagnosis.entity.MedicalRecord;
import org.springframework.data.domain.Page;

public interface MedicalRecordService {
    /**
     * 保存病历（新增或修改）
     */
    MedicalRecord saveRecord(MedicalRecord record);

    /**
     * 根据ID查询病历详情
     */
    MedicalRecord getDetail(Long id);

    /**
     * 根据挂号ID查询病历
     */
    MedicalRecord getByRegistrationId(Long registrationId);

    /**
     * 患者-我的病历列表（分页）
     */
    Page<MedicalRecord> getPatientList(Long patientId, Integer page, Integer size);

    /**
     * 医生-我写的病历列表（分页）
     */
    Page<MedicalRecord> getDoctorList(Long doctorId, Integer page, Integer size);
}