package com.neusoft.cloud_brain_diagnosis.service.impl;

import com.neusoft.cloud_brain_diagnosis.common.exception.BusinessException;
import com.neusoft.cloud_brain_diagnosis.entity.*;
import com.neusoft.cloud_brain_diagnosis.repository.*;
import com.neusoft.cloud_brain_diagnosis.service.ExaminationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExaminationServiceImpl implements ExaminationService {

    private final ExaminationRepository examinationRepository;
    private final ExaminationItemRepository examinationItemRepository;
    private final RegistrationRepository registrationRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;

    /**
     * 医生-开立检查
     */
    @Override
    @Transactional
    public Examination createExamination(Examination examination, Long doctorId) {
        // 1. 验证挂号记录
        if (examination.getRegistrationId() == null) {
            throw new BusinessException("挂号ID不能为空");
        }
        Registration registration = registrationRepository.findById(examination.getRegistrationId())
                .orElseThrow(() -> new BusinessException("挂号记录不存在"));
        validateDoctorCanOperate(registration, doctorId);

        // 2. 验证检查项目
        if (examination.getItemId() == null) {
            throw new BusinessException("请选择检查项目");
        }
        ExaminationItem item = examinationItemRepository.findById(examination.getItemId())
                .orElseThrow(() -> new BusinessException("检查项目不存在"));

        if (examinationRepository.existsByRegistrationIdAndItemIdAndStatus(
                examination.getRegistrationId(), examination.getItemId(), "待检查")) {
            throw new BusinessException("该检查项目已开立，请勿重复提交");
        }

        // 3. 验证患者和医生
        Patient patient = patientRepository.findById(registration.getPatientId())
                .orElseThrow(() -> new BusinessException("患者不存在"));
        Doctor doctor = doctorRepository.findById(registration.getDoctorId())
                .orElseThrow(() -> new BusinessException("医生不存在"));

        // 4. 填充冗余字段
        examination.setPatientId(registration.getPatientId());
        examination.setPatientName(patient.getName());
        examination.setDoctorId(registration.getDoctorId());
        examination.setDoctorName(doctor.getName());
        examination.setDepartmentId(registration.getDepartmentId());
        examination.setItemName(item.getName());
        examination.setType(item.getType());
        examination.setStatus("待检查");

        // 5. 保存
        return examinationRepository.save(examination);
    }

    @Override
    @Transactional
    public String cancelExamination(Long id, Long doctorId) {
        Examination examination = examinationRepository.findById(id)
                .orElseThrow(() -> new BusinessException("检查记录不存在"));
        if (!examination.getDoctorId().equals(doctorId)) {
            throw new BusinessException("无权撤销其他医生开立的检查");
        }
        if (!"待检查".equals(examination.getStatus())) {
            throw new BusinessException("只有待检查项目可以撤销");
        }
        examination.setStatus("已撤销");
        examinationRepository.save(examination);
        return "检查项目已撤销";
    }

    @Override
    public List<Examination> getByRegistrationId(Long registrationId, Long doctorId) {
        Registration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new BusinessException("挂号记录不存在"));
        if (!registration.getDoctorId().equals(doctorId)) {
            throw new BusinessException("无权查看其他医生的检查记录");
        }
        return examinationRepository.findByRegistrationIdOrderByCreateTimeDesc(registrationId);
    }

    private void validateDoctorCanOperate(Registration registration, Long doctorId) {
        if (!registration.getDoctorId().equals(doctorId)) {
            throw new BusinessException("无权操作其他医生的患者");
        }
        if (!"就诊中".equals(registration.getStatus())) {
            throw new BusinessException("请先开始看诊，只有就诊中的挂号可以开立检查");
        }
    }

    /**
     * 检查详情
     */
    @Override
    public Examination getDetail(Long id, Long userId, String role) {
        Examination examination = examinationRepository.findById(id)
                .orElseThrow(() -> new BusinessException("检查记录不存在"));
        if ("doctor".equals(role) && !examination.getDoctorId().equals(userId)) {
            throw new BusinessException("无权查看其他医生患者的检查记录");
        }
        if ("patient".equals(role) && !examination.getPatientId().equals(userId)) {
            throw new BusinessException("无权查看其他患者的检查记录");
        }
        if (!"doctor".equals(role) && !"patient".equals(role)
                && !"lab".equals(role) && !"admin".equals(role)) {
            throw new BusinessException("当前角色无权查看检查记录");
        }
        return examination;
    }

    /**
     * 患者-我的检查报告列表（分页）
     */
    @Override
    public Page<Examination> getPatientList(Long patientId, Integer page, Integer size) {
        PageRequest pageRequest = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createTime"));
        return examinationRepository.findByPatientIdOrderByCreateTimeDesc(patientId, pageRequest);
    }

    /**
     * 医生-我开的检查列表（分页）
     */
    @Override
    public Page<Examination> getDoctorList(Long doctorId, Integer page, Integer size) {
        PageRequest pageRequest = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createTime"));
        return examinationRepository.findByDoctorIdOrderByCreateTimeDesc(doctorId, pageRequest);
    }

    /**
     * 检验科-待检查列表（分页）
     */
    @Override
    public Page<Examination> getLabList(String status, Integer page, Integer size) {
        PageRequest pageRequest = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createTime"));
        if (status == null || status.isBlank() || "全部".equals(status)) {
            return examinationRepository.findAllByOrderByCreateTimeDesc(pageRequest);
        }
        if (!"待检查".equals(status) && !"已完成".equals(status)) {
            throw new BusinessException("检查状态参数不正确");
        }
        return examinationRepository.findByStatusOrderByCreateTimeDesc(status, pageRequest);
    }

    /**
     * 检验科-填写检查结果
     */
    @Override
    @Transactional
    public String updateResult(Long id, String result, String resultImages) {
        // 1. 查找检查记录
        Examination examination = examinationRepository.findById(id)
                .orElseThrow(() -> new BusinessException("检查记录不存在"));

        // 2. 验证状态
        if (!"待检查".equals(examination.getStatus())) {
            throw new BusinessException("当前状态不能修改结果，当前状态：" + examination.getStatus());
        }

        // 3. 验证结果内容
        if (result == null || result.trim().isEmpty()) {
            throw new BusinessException("检查结果不能为空");
        }

        // 4. 更新结果和状态
        examination.setResult(result.trim());
        examination.setResultImages(resultImages);
        examination.setStatus("已完成");
        examination.setCompleteTime(LocalDateTime.now());
        examinationRepository.save(examination);

        return "结果提交成功";
    }

    /**
     * 检查项目列表（支持按类型筛选）
     */
    @Override
    public List<ExaminationItem> getItemList(String type) {
        if (type != null && !type.isEmpty()) {
            return examinationItemRepository.findByType(type);
        }
        return examinationItemRepository.findAll();
    }
}
