package com.neusoft.cloud_brain_diagnosis.service.impl;

import com.neusoft.cloud_brain_diagnosis.common.exception.BusinessException;
import com.neusoft.cloud_brain_diagnosis.entity.Doctor;
import com.neusoft.cloud_brain_diagnosis.entity.Patient;
import com.neusoft.cloud_brain_diagnosis.entity.Prescription;
import com.neusoft.cloud_brain_diagnosis.entity.Registration;
import com.neusoft.cloud_brain_diagnosis.repository.DoctorRepository;
import com.neusoft.cloud_brain_diagnosis.repository.PatientRepository;
import com.neusoft.cloud_brain_diagnosis.repository.PrescriptionRepository;
import com.neusoft.cloud_brain_diagnosis.repository.RegistrationRepository;
import com.neusoft.cloud_brain_diagnosis.service.PrescriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PrescriptionServiceImpl implements PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;
    private final RegistrationRepository registrationRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;

    /**
     * 医生-开具处方
     */
    @Override
    @Transactional
    public Prescription createPrescription(Prescription prescription) {
        // 1. 验证挂号记录
        if (prescription.getRegistrationId() == null) {
            throw new BusinessException("挂号ID不能为空");
        }
        Registration registration = registrationRepository.findById(prescription.getRegistrationId())
                .orElseThrow(() -> new BusinessException("挂号记录不存在"));

        // 2. 验证患者和医生
        Patient patient = patientRepository.findById(registration.getPatientId())
                .orElseThrow(() -> new BusinessException("患者不存在"));
        Doctor doctor = doctorRepository.findById(registration.getDoctorId())
                .orElseThrow(() -> new BusinessException("医生不存在"));

        // 3. 验证药品列表不能为空
        if (prescription.getDrugs() == null || prescription.getDrugs().isEmpty()) {
            throw new BusinessException("请添加药品");
        }

        // 4. 如果总金额为空，设置为0
        if (prescription.getTotalAmount() == null) {
            prescription.setTotalAmount(BigDecimal.ZERO);
        }

        // 5. 填充冗余字段
        prescription.setPatientId(registration.getPatientId());
        prescription.setPatientName(patient.getName());
        prescription.setDoctorId(registration.getDoctorId());
        prescription.setDoctorName(doctor.getName());
        prescription.setDepartmentId(registration.getDepartmentId());
        prescription.setStatus("待发药");

        // 6. 保存
        return prescriptionRepository.save(prescription);
    }

    /**
     * 处方详情
     */
    @Override
    public Prescription getDetail(Long id) {
        return prescriptionRepository.findById(id)
                .orElseThrow(() -> new BusinessException("处方不存在"));
    }

    /**
     * 患者-我的处方列表（分页）
     */
    @Override
    public Page<Prescription> getPatientList(Long patientId, Integer page, Integer size) {
        PageRequest pageRequest = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createTime"));
        return prescriptionRepository.findByPatientIdOrderByCreateTimeDesc(patientId, pageRequest);
    }

    /**
     * 医生-我开的处方列表（分页）
     */
    @Override
    public Page<Prescription> getDoctorList(Long doctorId, Integer page, Integer size) {
        PageRequest pageRequest = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createTime"));
        return prescriptionRepository.findByDoctorIdOrderByCreateTimeDesc(doctorId, pageRequest);
    }

    /**
     * 药房-待发药处方列表（分页）
     */
    @Override
    public Page<Prescription> getPharmacyList(Integer page, Integer size) {
        PageRequest pageRequest = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createTime"));
        return prescriptionRepository.findByStatusOrderByCreateTimeDesc("待发药", pageRequest);
    }

    /**
     * 药房-发药
     */
    @Override
    @Transactional
    public String dispense(Long id) {
        // 1. 查找处方
        Prescription prescription = prescriptionRepository.findById(id)
                .orElseThrow(() -> new BusinessException("处方不存在"));

        // 2. 验证状态
        if (!"待发药".equals(prescription.getStatus())) {
            throw new BusinessException("当前状态不能发药，当前状态：" + prescription.getStatus());
        }

        // 3. 更新状态和发药时间
        prescription.setStatus("已发药");
        prescription.setDispenseTime(LocalDateTime.now());
        prescriptionRepository.save(prescription);

        return "发药成功";
    }
}
