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
import java.time.LocalDateTime;
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
        Map<Long, Integer> medicineQuantities = parseMedicineQuantities(prescription.getDrugs());
        if (medicineQuantities.isEmpty()) {
            throw new BusinessException("请添加药品");
        }
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (Map.Entry<Long, Integer> entry : medicineQuantities.entrySet()) {
            Medicine medicine = medicineRepository.findById(entry.getKey())
                    .orElseThrow(() -> new BusinessException("药品不存在，药品ID：" + entry.getKey()));
            int stock = medicine.getStock() == null ? 0 : medicine.getStock();
            if (stock < entry.getValue()) {
                throw new BusinessException("药品库存不足：" + medicine.getName());
            }
            if (medicine.getPrice() != null) {
                totalAmount = totalAmount.add(medicine.getPrice().multiply(BigDecimal.valueOf(entry.getValue())));
            }
        }
        for (Map.Entry<Long, Integer> entry : medicineQuantities.entrySet()) {
            Medicine medicine = medicineRepository.findById(entry.getKey())
                    .orElseThrow(() -> new BusinessException("药品不存在，药品ID：" + entry.getKey()));
            medicine.setStock((medicine.getStock() == null ? 0 : medicine.getStock()) - entry.getValue());
            medicineRepository.save(medicine);
        }

        // 4. 如果总金额为空，设置为0
        if (prescription.getTotalAmount() == null) {
            prescription.setTotalAmount(totalAmount);
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
    public Prescription getDetail(Long id, Long userId, String role) {
        Prescription prescription = prescriptionRepository.findById(id)
                .orElseThrow(() -> new BusinessException("处方不存在"));
        if ("patient".equals(role) && !prescription.getPatientId().equals(userId)) {
            throw new BusinessException("无权查看其他患者的处方");
        }
        if ("doctor".equals(role) && !prescription.getDoctorId().equals(userId)) {
            throw new BusinessException("无权查看其他医生的处方");
        }
        if (!"patient".equals(role) && !"doctor".equals(role)
                && !"admin".equals(role) && !"pharmacy".equals(role)) {
            throw new BusinessException("当前角色无权查看处方详情");
        }
        return prescription;
    }

    @Override
    @Transactional
    public String cancelPrescription(Long id, Long doctorId) {
        Prescription prescription = prescriptionRepository.findById(id)
                .orElseThrow(() -> new BusinessException("处方不存在"));
        if (!prescription.getDoctorId().equals(doctorId)) {
            throw new BusinessException("无权撤销其他医生的处方");
        }
        if (!"待发药".equals(prescription.getStatus())) {
            throw new BusinessException("只有待发药处方可以撤销");
        }
        try {
            for (Map.Entry<Long, Integer> entry : parseMedicineQuantities(prescription.getDrugs()).entrySet()) {
                Medicine medicine = medicineRepository.findById(entry.getKey())
                        .orElseThrow(() -> new BusinessException("药品不存在，药品ID：" + entry.getKey()));
                medicine.setStock((medicine.getStock() == null ? 0 : medicine.getStock()) + entry.getValue());
                medicineRepository.save(medicine);
            }
        } catch (BusinessException e) {
            throw new BusinessException("处方库存返还失败：" + e.getMessage());
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
            throw new BusinessException("无权查看其他医生患者的处方");
        }
        return prescriptionRepository.findByRegistrationIdAndStatusNotOrderByCreateTimeDesc(registrationId, "已撤销");
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
     * 药房-处方列表（分页，可按状态筛选）
     */
    @Override
    public Page<Prescription> getPharmacyList(String status, Integer page, Integer size) {
        PageRequest pageRequest = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createTime"));
        if (status == null || status.trim().isEmpty() || "全部".equals(status.trim())) {
            return prescriptionRepository.findAllByOrderByCreateTimeDesc(pageRequest);
        }
        String trimmedStatus = status.trim();
        if (!"待发药".equals(trimmedStatus) && !"已发药".equals(trimmedStatus) && !"已撤销".equals(trimmedStatus)) {
            throw new BusinessException("状态参数不正确");
        }
        return prescriptionRepository.findByStatusOrderByCreateTimeDesc(trimmedStatus, pageRequest);
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

        // 3. 根据处方药品清单扣减库存。这里必须先全部校验，再统一扣减；
        //    否则某一种药库存不足时，可能出现前几种药已经扣了但处方没有发出的脏数据。
        Map<Long, Integer> medicineQuantities = parseMedicineQuantities(prescription.getDrugs());
        for (Map.Entry<Long, Integer> entry : medicineQuantities.entrySet()) {
            Medicine medicine = medicineRepository.findById(entry.getKey())
                    .orElseThrow(() -> new BusinessException("药品不存在，药品ID：" + entry.getKey()));
            int currentStock = medicine.getStock() == null ? 0 : medicine.getStock();
            int needQuantity = entry.getValue();
            if (currentStock < needQuantity) {
                throw new BusinessException("药品库存不足：" + medicine.getName()
                        + "，当前库存 " + currentStock + medicine.getUnit()
                        + "，需要 " + needQuantity + medicine.getUnit());
            }
        }

        for (Map.Entry<Long, Integer> entry : medicineQuantities.entrySet()) {
            Medicine medicine = medicineRepository.findById(entry.getKey())
                    .orElseThrow(() -> new BusinessException("药品不存在，药品ID：" + entry.getKey()));
            medicine.setStock((medicine.getStock() == null ? 0 : medicine.getStock()) - entry.getValue());
            medicineRepository.save(medicine);
        }

        // 4. 更新状态和发药时间
        prescription.setStatus("已发药");
        prescription.setDispenseTime(LocalDateTime.now());
        prescriptionRepository.save(prescription);

        return "发药成功";
    }

    private Map<Long, Integer> parseMedicineQuantities(String drugsJson) {
        try {
            JsonNode root = objectMapper.readTree(drugsJson == null ? "[]" : drugsJson);
            if (!root.isArray()) {
                throw new BusinessException("处方药品明细格式错误");
            }

            Map<Long, Integer> result = new LinkedHashMap<>();
            for (JsonNode drug : root) {
                long medicineId = drug.hasNonNull("medicineId")
                        ? drug.get("medicineId").asLong()
                        : drug.path("id").asLong(0);
                int quantity = drug.path("quantity").asInt(0);
                if (medicineId <= 0) {
                    throw new BusinessException("处方药品缺少药品ID");
                }
                if (quantity <= 0) {
                    throw new BusinessException("处方药品数量必须大于0，药品ID：" + medicineId);
                }
                result.merge(medicineId, quantity, Integer::sum);
            }
            return result;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("处方药品明细解析失败");
        }
    }

    private void validateDoctorCanOperate(Registration registration, Long doctorId) {
        if (!registration.getDoctorId().equals(doctorId)) {
            throw new BusinessException("无权操作其他医生的患者");
        }
        if (!"就诊中".equals(registration.getStatus())) {
            throw new BusinessException("请先开始看诊");
        }
    }
}
