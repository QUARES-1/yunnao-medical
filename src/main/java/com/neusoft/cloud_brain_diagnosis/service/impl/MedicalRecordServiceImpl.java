package com.neusoft.cloud_brain_diagnosis.service.impl;

import com.neusoft.cloud_brain_diagnosis.common.exception.BusinessException;
import com.neusoft.cloud_brain_diagnosis.entity.Doctor;
import com.neusoft.cloud_brain_diagnosis.entity.MedicalRecord;
import com.neusoft.cloud_brain_diagnosis.entity.Patient;
import com.neusoft.cloud_brain_diagnosis.entity.Registration;
import com.neusoft.cloud_brain_diagnosis.repository.DoctorRepository;
import com.neusoft.cloud_brain_diagnosis.repository.MedicalRecordRepository;
import com.neusoft.cloud_brain_diagnosis.repository.PatientRepository;
import com.neusoft.cloud_brain_diagnosis.repository.RegistrationRepository;
import com.neusoft.cloud_brain_diagnosis.service.MedicalRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MedicalRecordServiceImpl implements MedicalRecordService {

    private final MedicalRecordRepository medicalRecordRepository;
    private final RegistrationRepository registrationRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;

    /**
     * 保存病历
     * - 如果有ID，就是修改
     * - 如果没有ID，就是新增，需要从挂号记录获取患者、医生信息
     */
    @Override
    @Transactional
    public MedicalRecord saveRecord(MedicalRecord record, Long doctorId) {
        // ========== 修改病历 ==========
        if (record.getId() != null) {
            MedicalRecord exist = medicalRecordRepository.findById(record.getId())
                    .orElseThrow(() -> new BusinessException("病历不存在"));

            Registration registration = registrationRepository.findById(exist.getRegistrationId())
                    .orElseThrow(() -> new BusinessException("挂号记录不存在"));
            validateDoctorCanOperate(registration, doctorId);

            // 只更新非空字段
            if (record.getChiefComplaint() != null) {
                exist.setChiefComplaint(record.getChiefComplaint());
            }
            if (record.getPresentIllness() != null) {
                exist.setPresentIllness(record.getPresentIllness());
            }
            if (record.getPastHistory() != null) {
                exist.setPastHistory(record.getPastHistory());
            }
            if (record.getPhysicalExamination() != null) {
                exist.setPhysicalExamination(record.getPhysicalExamination());
            }
            if (record.getDiagnosis() != null) {
                exist.setDiagnosis(record.getDiagnosis());
            }
            if (record.getTreatment() != null) {
                exist.setTreatment(record.getTreatment());
            }

            return medicalRecordRepository.save(exist);
        }

        // ========== 新增病历 ==========
        // 1. 验证挂号记录是否存在
        if (record.getRegistrationId() == null) {
            throw new BusinessException("挂号ID不能为空");
        }
        Registration registration = registrationRepository.findById(record.getRegistrationId())
                .orElseThrow(() -> new BusinessException("挂号记录不存在"));
        validateDoctorCanOperate(registration, doctorId);

        // 2. 验证患者和医生
        Patient patient = patientRepository.findById(registration.getPatientId())
                .orElseThrow(() -> new BusinessException("患者不存在"));
        Doctor doctor = doctorRepository.findById(registration.getDoctorId())
                .orElseThrow(() -> new BusinessException("医生不存在"));

        // 3. 检查该挂号是否已经有病历了（一个挂号对应一份病历）
        MedicalRecord existRecord = medicalRecordRepository.findByRegistrationId(record.getRegistrationId()).orElse(null);
        if (existRecord != null) {
            // 如果已经有了，就更新，而不是新增
            if (record.getChiefComplaint() != null) existRecord.setChiefComplaint(record.getChiefComplaint());
            if (record.getPresentIllness() != null) existRecord.setPresentIllness(record.getPresentIllness());
            if (record.getPastHistory() != null) existRecord.setPastHistory(record.getPastHistory());
            if (record.getPhysicalExamination() != null) existRecord.setPhysicalExamination(record.getPhysicalExamination());
            if (record.getDiagnosis() != null) existRecord.setDiagnosis(record.getDiagnosis());
            if (record.getTreatment() != null) existRecord.setTreatment(record.getTreatment());
            return medicalRecordRepository.save(existRecord);
        }

        // 4. 填充冗余字段
        record.setPatientId(registration.getPatientId());
        record.setPatientName(patient.getName());
        record.setDoctorId(registration.getDoctorId());
        record.setDoctorName(doctor.getName());
        record.setDepartmentId(registration.getDepartmentId());

        // 5. 保存
        return medicalRecordRepository.save(record);
    }

    private void validateDoctorCanOperate(Registration registration, Long doctorId) {
        if (!registration.getDoctorId().equals(doctorId)) {
            throw new BusinessException("无权操作其他医生的患者");
        }
        if (!"就诊中".equals(registration.getStatus())) {
            throw new BusinessException("请先开始看诊，只有就诊中的挂号可以编辑病历");
        }
    }

    /**
     * 根据ID查询病历详情
     */
    @Override
    public MedicalRecord getDetail(Long id, Long userId, String role) {
        MedicalRecord record = medicalRecordRepository.findById(id)
                .orElseThrow(() -> new BusinessException("病历不存在"));
        validateReadAccess(record.getRegistrationId(), userId, role);
        return record;
    }

    /**
     * 根据挂号ID查询病历
     */
    @Override
    public MedicalRecord getByRegistrationId(Long registrationId, Long userId, String role) {
        validateReadAccess(registrationId, userId, role);
        return medicalRecordRepository.findByRegistrationId(registrationId).orElse(null);
    }

    private void validateReadAccess(Long registrationId, Long userId, String role) {
        Registration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new BusinessException("挂号记录不存在"));
        if ("doctor".equals(role) && !registration.getDoctorId().equals(userId)) {
            throw new BusinessException("该患者不属于当前医生，无权查看病历");
        }
        if ("patient".equals(role) && !registration.getPatientId().equals(userId)) {
            throw new BusinessException("无权查看其他患者的病历");
        }
        if (!"doctor".equals(role) && !"patient".equals(role) && !"admin".equals(role)) {
            throw new BusinessException("当前角色无权查看病历");
        }
    }

    /**
     * 患者-我的病历列表（分页）
     */
    @Override
    public Page<MedicalRecord> getPatientList(Long patientId, Integer page, Integer size) {
        PageRequest pageRequest = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createTime"));
        return medicalRecordRepository.findByPatientIdOrderByCreateTimeDesc(patientId, pageRequest);
    }

    /**
     * 医生-我写的病历列表（分页）
     */
    @Override
    public Page<MedicalRecord> getDoctorList(Long doctorId, Integer page, Integer size) {
        PageRequest pageRequest = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createTime"));
        return medicalRecordRepository.findByDoctorIdOrderByCreateTimeDesc(doctorId, pageRequest);
    }
}
