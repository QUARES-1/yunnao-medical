package com.neusoft.cloud_brain_diagnosis.service.impl;

import com.neusoft.cloud_brain_diagnosis.common.exception.BusinessException;
import com.neusoft.cloud_brain_diagnosis.entity.Doctor;
import com.neusoft.cloud_brain_diagnosis.entity.Patient;
import com.neusoft.cloud_brain_diagnosis.entity.Registration;
import com.neusoft.cloud_brain_diagnosis.repository.DoctorRepository;
import com.neusoft.cloud_brain_diagnosis.repository.MedicalRecordRepository;
import com.neusoft.cloud_brain_diagnosis.repository.PatientRepository;
import com.neusoft.cloud_brain_diagnosis.repository.RegistrationRepository;
import com.neusoft.cloud_brain_diagnosis.service.RegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RegistrationServiceImpl implements RegistrationService {

    private final RegistrationRepository registrationRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final MedicalRecordRepository medicalRecordRepository;

    // ========================================
    // 患者端方法
    // ========================================

    /**
     * 创建挂号
     */
    @Override
    @Transactional
    public Registration createRegistration(Registration registration) {
        // 1. 验证患者是否存在
        Patient patient = patientRepository.findById(registration.getPatientId())
                .orElseThrow(() -> new BusinessException("患者不存在"));

        // 2. 验证医生是否存在
        Doctor doctor = doctorRepository.findById(registration.getDoctorId())
                .orElseThrow(() -> new BusinessException("医生不存在"));

        // 3. 验证挂号日期和时间段
        if (registration.getRegistrationDate() == null) {
            throw new BusinessException("请选择挂号日期");
        }
        if (registration.getTimeSlot() == null || registration.getTimeSlot().isEmpty()) {
            throw new BusinessException("请选择时间段");
        }

        // 4. 验证日期不能早于今天
        if (registration.getRegistrationDate().isBefore(LocalDate.now())) {
            throw new BusinessException("不能预约过去的日期");
        }

        // 5. 防止重复挂号：同一天同一患者不能挂同一个医生同一时段
        long count = registrationRepository.countByDoctorIdAndRegistrationDateAndTimeSlot(
                registration.getDoctorId(), registration.getRegistrationDate(), registration.getTimeSlot());
        // 检查该患者是否已在该时段挂过该医生
        // 使用更精确的查找方式
        List<Registration> existingRegs = registrationRepository.findByDoctorIdAndRegistrationDateOrderByCreateTimeAsc(
                registration.getDoctorId(), registration.getRegistrationDate());
        for (Registration reg : existingRegs) {
            if (reg.getPatientId().equals(registration.getPatientId())
                    && reg.getTimeSlot().equals(registration.getTimeSlot())
                    && !"已取消".equals(reg.getStatus())) {
                throw new BusinessException("您已在该时段挂过该医生，请勿重复挂号");
            }
        }

        // 6. 填充冗余字段
        registration.setPatientName(patient.getName());
        registration.setDoctorName(doctor.getName());
        registration.setDepartmentId(doctor.getDepartmentId());
        registration.setDepartmentName(doctor.getDepartmentName());
        registration.setStatus("待就诊");

        // 7. 保存并返回
        return registrationRepository.save(registration);
    }

    /**
     * 患者-我的挂号记录列表（分页）
     */
    @Override
    public Page<Registration> getPatientRegistrationList(Long patientId, String status, Integer page, Integer size) {
        PageRequest pageRequest = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createTime"));

        // 如果传了状态，按状态筛选；否则查全部
        if (status != null && !status.isEmpty()) {
            return registrationRepository.findByPatientIdAndStatusOrderByCreateTimeDesc(patientId, status, pageRequest);
        }
        return registrationRepository.findByPatientIdOrderByCreateTimeDesc(patientId, pageRequest);
    }

    /**
     * 挂号详情
     */
    @Override
    public Registration getDetail(Long id, Long userId, String role) {
        Registration registration = registrationRepository.findById(id)
                .orElseThrow(() -> new BusinessException("挂号记录不存在"));
        if ("doctor".equals(role) && !registration.getDoctorId().equals(userId)) {
            throw new BusinessException("该患者未挂号给当前医生，无权查看");
        }
        if ("patient".equals(role) && !registration.getPatientId().equals(userId)) {
            throw new BusinessException("无权查看其他患者的挂号记录");
        }
        if (!"doctor".equals(role) && !"patient".equals(role) && !"admin".equals(role)) {
            throw new BusinessException("当前角色无权查看挂号详情");
        }
        Patient patient = patientRepository.findById(registration.getPatientId())
                .orElseThrow(() -> new BusinessException("患者不存在"));
        registration.setPatientGender(patient.getGender());
        registration.setPatientAge(patient.getAge());
        registration.setPatientPhone(patient.getPhone());
        registration.setPatientAllergyHistory(patient.getAllergyHistory());
        return registration;
    }

    /**
     * 患者-取消挂号
     */
    @Override
    @Transactional
    public String cancelRegistration(Long id, Long patientId) {
        // 1. 查找挂号记录
        Registration registration = registrationRepository.findById(id)
                .orElseThrow(() -> new BusinessException("挂号记录不存在"));

        // 2. 验证是否是本人的挂号
        if (!registration.getPatientId().equals(patientId)) {
            throw new BusinessException("无权操作他人的挂号记录");
        }

        // 3. 验证状态：只有待就诊状态才能取消
        if (!"待就诊".equals(registration.getStatus())) {
            throw new BusinessException("只有待就诊状态的挂号才能取消");
        }

        // 4. 更新状态
        registration.setStatus("已取消");
        registrationRepository.save(registration);

        return "取消挂号成功";
    }

    // ========================================
    // 医生端方法
    // ========================================

    /**
     * 医生-今日挂号列表
     */
    @Override
    public List<Registration> getDoctorTodayList(Long doctorId) {
        LocalDate today = LocalDate.now();
        return registrationRepository.findByDoctorIdAndRegistrationDateOrderByCreateTimeAsc(doctorId, today);
    }

    /**
     * 医生-历史挂号列表（分页）
     */
    @Override
    public Page<Registration> getDoctorList(Long doctorId, String keyword, String status, Integer page, Integer size) {
        PageRequest pageRequest = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createTime"));
        return registrationRepository.searchDoctorHistory(doctorId, keyword, status, pageRequest);
    }

    /**
     * 医生-开始看诊
     */
    @Override
    @Transactional
    public String startConsultation(Long id, Long doctorId) {
        // 1. 查找挂号记录
        Registration registration = registrationRepository.findById(id)
                .orElseThrow(() -> new BusinessException("挂号记录不存在"));

        // 2. 验证是否是该医生的患者
        if (!registration.getDoctorId().equals(doctorId)) {
            throw new BusinessException("无权操作其他医生的患者");
        }

        // 3. 验证状态：只能从待就诊开始
        if (!"待就诊".equals(registration.getStatus())) {
            throw new BusinessException("当前状态不能开始看诊，当前状态：" + registration.getStatus());
        }

        // 4. 更新状态为就诊中
        registration.setStatus("就诊中");
        registrationRepository.save(registration);

        return "开始看诊";
    }

    /**
     * 医生-完成看诊
     */
    @Override
    @Transactional
    public String completeConsultation(Long id, Long doctorId) {
        // 1. 查找挂号记录
        Registration registration = registrationRepository.findById(id)
                .orElseThrow(() -> new BusinessException("挂号记录不存在"));

        // 2. 验证是否是该医生的患者
        if (!registration.getDoctorId().equals(doctorId)) {
            throw new BusinessException("无权操作其他医生的患者");
        }

        // 3. 验证状态：只能从就诊中完成
        if (!"就诊中".equals(registration.getStatus())) {
            throw new BusinessException("当前状态不能完成看诊，当前状态：" + registration.getStatus());
        }

        var medicalRecord = medicalRecordRepository.findByRegistrationId(id)
                .orElseThrow(() -> new BusinessException("请先保存病历，再完成看诊"));
        if (isBlank(medicalRecord.getChiefComplaint())
                || isBlank(medicalRecord.getDiagnosis())
                || isBlank(medicalRecord.getTreatment())) {
            throw new BusinessException("病历未填写完整，请补充主诉、诊断结果和治疗意见");
        }

        // 4. 更新状态为已就诊
        registration.setStatus("已就诊");
        registrationRepository.save(registration);

        return "看诊完成";
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
