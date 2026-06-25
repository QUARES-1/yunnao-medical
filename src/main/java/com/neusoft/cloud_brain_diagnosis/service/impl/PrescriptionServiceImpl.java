package com.neusoft.cloud_brain_diagnosis.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neusoft.cloud_brain_diagnosis.common.exception.BusinessException;
import com.neusoft.cloud_brain_diagnosis.entity.Doctor;
import com.neusoft.cloud_brain_diagnosis.entity.Medicine;
import com.neusoft.cloud_brain_diagnosis.entity.Patient;
import com.neusoft.cloud_brain_diagnosis.entity.Prescription;
import com.neusoft.cloud_brain_diagnosis.entity.Registration;
import com.neusoft.cloud_brain_diagnosis.repository.DoctorRepository;
import com.neusoft.cloud_brain_diagnosis.repository.MedicineRepository;
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
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PrescriptionServiceImpl implements PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;
    private final RegistrationRepository registrationRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final MedicineRepository medicineRepository;
    private final ObjectMapper objectMapper;

    /**
     * 医生-开具处方
     */
    @Override
    @Transactional
    public Prescription createPrescription(Prescription prescription, Long doctorId) {
        // 1. 验证挂号记录
        if (prescription.getRegistrationId() == null) {
            throw new BusinessException("挂号ID不能为空");
        }
        Registration registration = registrationRepository.findById(prescription.getRegistrationId())
                .orElseThrow(() -> new BusinessException("挂号记录不存在"));
        validateDoctorCanOperate(registration, doctorId);

        // 2. 验证患者和医生
        Patient patient = patientRepository.findById(registration.getPatientId())
                .orElseThrow(() -> new BusinessException("患者不存在"));
        Doctor doctor = doctorRepository.findById(registration.getDoctorId())
                .orElseThrow(() -> new BusinessException("医生不存在"));

        // 3. 验证药品列表不能为空
        if (prescription.getDrugs() == null || prescription.getDrugs().isEmpty()) {
            throw new BusinessException("请添加药品");
        }

        // 价格、库存和药品信息必须以后端数据库为准，不能信任前端传值。
        List<Map<String, Object>> safeDrugs = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        try {
            JsonNode drugs = objectMapper.readTree(prescription.getDrugs());
            if (!drugs.isArray() || drugs.isEmpty()) {
                throw new BusinessException("请添加药品");
            }
            for (JsonNode drug : drugs) {
                Long medicineId = drug.path("medicineId").isNumber() ? drug.path("medicineId").asLong() : null;
                int quantity = drug.path("quantity").asInt(0);
                String dosage = drug.path("dosage").asText("").trim();
                if (medicineId == null || quantity <= 0 || dosage.isEmpty()) {
                    throw new BusinessException("药品、数量和用法用量不能为空");
                }

                Medicine medicine = medicineRepository.findById(medicineId)
                        .orElseThrow(() -> new BusinessException("药品不存在或已下架"));
                if (medicine.getStock() == null || medicine.getStock() < quantity) {
                    throw new BusinessException(medicine.getName() + "库存不足，当前库存：" + medicine.getStock());
                }

                medicine.setStock(medicine.getStock() - quantity);
                medicineRepository.save(medicine);
                totalAmount = totalAmount.add(medicine.getPrice().multiply(BigDecimal.valueOf(quantity)));

                Map<String, Object> safeDrug = new LinkedHashMap<>();
                safeDrug.put("medicineId", medicine.getId());
                safeDrug.put("medicineName", medicine.getName());
                safeDrug.put("specification", medicine.getSpecification());
                safeDrug.put("quantity", quantity);
                safeDrug.put("unit", medicine.getUnit());
                safeDrug.put("dosage", dosage);
                safeDrugs.add(safeDrug);
            }
            prescription.setDrugs(objectMapper.writeValueAsString(safeDrugs));
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("处方药品数据格式不正确");
        }
        prescription.setTotalAmount(totalAmount.setScale(2, RoundingMode.HALF_UP));

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

    @Override
    @Transactional
    public String cancelPrescription(Long id, Long doctorId) {
        Prescription prescription = prescriptionRepository.findById(id)
                .orElseThrow(() -> new BusinessException("处方不存在"));
        if (!prescription.getDoctorId().equals(doctorId)) {
            throw new BusinessException("无权撤销其他医生开具的处方");
        }
        if (!"待发药".equals(prescription.getStatus())) {
            throw new BusinessException("只有待发药处方可以撤销");
        }
        try {
            JsonNode drugs = objectMapper.readTree(prescription.getDrugs());
            for (JsonNode drug : drugs) {
                Long medicineId = drug.path("medicineId").asLong();
                int quantity = drug.path("quantity").asInt();
                Medicine medicine = medicineRepository.findById(medicineId)
                        .orElseThrow(() -> new BusinessException("处方中的药品不存在"));
                medicine.setStock((medicine.getStock() == null ? 0 : medicine.getStock()) + quantity);
                medicineRepository.save(medicine);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("处方库存返还失败，请联系管理员");
        }
        prescription.setStatus("已撤销");
        prescriptionRepository.save(prescription);
        return "处方已撤销，药品库存已返还";
    }

    @Override
    public List<Prescription> getByRegistrationId(Long registrationId, Long doctorId) {
        Registration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new BusinessException("挂号记录不存在"));
        if (!registration.getDoctorId().equals(doctorId)) {
            throw new BusinessException("无权查看其他医生的处方记录");
        }
        return prescriptionRepository.findByRegistrationIdAndStatusNotOrderByCreateTimeDesc(registrationId, "已撤销");
    }

    private void validateDoctorCanOperate(Registration registration, Long doctorId) {
        if (!registration.getDoctorId().equals(doctorId)) {
            throw new BusinessException("无权操作其他医生的患者");
        }
        if (!"就诊中".equals(registration.getStatus())) {
            throw new BusinessException("请先开始看诊，只有就诊中的挂号可以开具处方");
        }
    }

    /**
     * 处方详情
     */
    @Override
    public Prescription getDetail(Long id, Long userId, String role) {
        Prescription prescription = prescriptionRepository.findById(id)
                .orElseThrow(() -> new BusinessException("处方不存在"));
        if ("doctor".equals(role) && !prescription.getDoctorId().equals(userId)) {
            throw new BusinessException("无权查看其他医生患者的处方");
        }
        if ("patient".equals(role) && !prescription.getPatientId().equals(userId)) {
            throw new BusinessException("无权查看其他患者的处方");
        }
        if (!"doctor".equals(role) && !"patient".equals(role)
                && !"pharmacy".equals(role) && !"admin".equals(role)) {
            throw new BusinessException("当前角色无权查看处方");
        }
        return prescription;
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
    public Page<Prescription> getPharmacyList(String status, Integer page, Integer size) {
        PageRequest pageRequest = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createTime"));
        if (status == null || status.isBlank() || "全部".equals(status)) {
            return prescriptionRepository.findAllByOrderByCreateTimeDesc(pageRequest);
        }
        if (!"待发药".equals(status) && !"已发药".equals(status)) {
            throw new BusinessException("处方状态参数不正确");
        }
        return prescriptionRepository.findByStatusOrderByCreateTimeDesc(status, pageRequest);
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
