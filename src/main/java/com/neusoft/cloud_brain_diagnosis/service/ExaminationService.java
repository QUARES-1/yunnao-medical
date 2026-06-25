package com.neusoft.cloud_brain_diagnosis.service;

import com.neusoft.cloud_brain_diagnosis.entity.Examination;
import com.neusoft.cloud_brain_diagnosis.entity.ExaminationItem;
import org.springframework.data.domain.Page;

import java.util.List;

public interface ExaminationService {
    /**
     * 医生-开立检查
     */
    Examination createExamination(Examination examination, Long doctorId);

    String cancelExamination(Long id, Long doctorId);

    List<Examination> getByRegistrationId(Long registrationId, Long doctorId);

    /**
     * 检查详情
     */
    Examination getDetail(Long id, Long userId, String role);

    /**
     * 患者-我的检查报告列表（分页）
     */
    Page<Examination> getPatientList(Long patientId, Integer page, Integer size);

    /**
     * 医生-我开的检查列表（分页）
     */
    Page<Examination> getDoctorList(Long doctorId, Integer page, Integer size);

    /**
     * 检验科-待检查列表（分页）
     */
    Page<Examination> getLabList(String status, Integer page, Integer size);

    /**
     * 检验科-填写检查结果
     */
    String updateResult(Long id, String result, String resultImages);

    /**
     * 检查项目列表（支持按类型筛选）
     */
    List<ExaminationItem> getItemList(String type);
}
