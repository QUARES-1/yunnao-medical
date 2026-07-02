package com.neusoft.cloud_brain_diagnosis.config;

import cn.hutool.json.JSONUtil;
import com.neusoft.cloud_brain_diagnosis.entity.*;
import com.neusoft.cloud_brain_diagnosis.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 医生端联调演示数据初始化。
 *
 * 说明：
 * 1. 前端不直接连接数据库，医生端统一访问 http://localhost:8080。
 * 2. 后端通过 application.yaml 连接金仓数据库。
 * 3. 当金仓为空库或缺少关键基础数据时，自动补充一套真实感演示数据，便于医生端、药房端、患者端联调。
 */
@Component
@RequiredArgsConstructor
public class DoctorDemoDataInitializer implements CommandLineRunner {

    private final DepartmentRepository departmentRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final MedicineRepository medicineRepository;
    private final ExaminationItemRepository examinationItemRepository;
    private final RegistrationRepository registrationRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final PrescriptionRepository prescriptionRepository;

    @Override
    public void run(String... args) {
        Map<String, Department> departments = ensureDepartments();
        List<Doctor> doctors = ensureDoctors(departments);
        List<Patient> patients = ensurePatients();
        List<Medicine> medicines = ensureMedicines();
        ensureExaminationItems();
        List<Registration> registrations = ensureRegistrations(doctors, patients, departments);
        ensureMedicalRecords(registrations);
        ensurePrescriptions(registrations, medicines);
        normalizeDoctorDemoData(doctors, patients, departments);
    }

    private Map<String, Department> ensureDepartments() {
        List<String> names = List.of("心血管内科", "呼吸内科", "儿科", "骨科", "眼科", "耳鼻喉科", "口腔科", "皮肤科", "神经内科", "消化内科");
        Map<String, Department> existing = departmentRepository.findAll().stream()
                .collect(Collectors.toMap(Department::getName, Function.identity(), (a, b) -> a));
        int sort = 1;
        for (String name : names) {
            if (!existing.containsKey(name)) {
                Department d = new Department();
                d.setName(name);
                d.setDescription(name + "常见病、慢性病与专科诊疗服务");
                d.setSort(sort);
                existing.put(name, departmentRepository.save(d));
            }
            sort++;
        }
        return existing;
    }

    private List<Doctor> ensureDoctors(Map<String, Department> departments) {
        List<DoctorSeed> seeds = List.of(
                new DoctorSeed("doctor01", "张建国", "主任医师", "心血管内科", "高血压、冠心病、心律失常"),
                new DoctorSeed("doctor02", "林知远", "主任医师", "心血管内科", "胸痛、心悸、冠脉疾病"),
                new DoctorSeed("doctor03", "温以宁", "副主任医师", "呼吸内科", "咳嗽、哮喘、肺部感染"),
                new DoctorSeed("doctor04", "苏清禾", "主治医师", "儿科", "儿童呼吸道感染、消化系统疾病"),
                new DoctorSeed("doctor05", "陈屿安", "副主任医师", "骨科", "颈肩腰腿痛、运动损伤"),
                new DoctorSeed("doctor06", "顾云舒", "主任医师", "眼科", "近视防控、白内障、眼底病"),
                new DoctorSeed("doctor07", "周明川", "副主任医师", "呼吸内科", "慢阻肺、支气管炎、肺结节"),
                new DoctorSeed("doctor08", "韩沐", "主治医师", "消化内科", "胃炎、反流、腹痛腹泻")
        );
        List<Doctor> result = new ArrayList<>();
        for (DoctorSeed seed : seeds) {
            Doctor doctor = doctorRepository.findByUsername(seed.username()).orElseGet(() -> {
                Doctor d = new Doctor();
                d.setUsername(seed.username());
                d.setPassword("123456");
                return d;
            });
            Department department = departments.get(seed.departmentName());
            doctor.setName(seed.name());
            doctor.setTitle(seed.title());
            doctor.setDepartmentId(department == null ? null : department.getId());
            doctor.setDepartmentName(seed.departmentName());
            doctor.setSpecialty(seed.specialty());
            doctor.setPhone("1380000" + String.format("%04d", Math.abs(seed.username().hashCode()) % 10000));
            doctor.setIntroduction("从事临床工作多年，擅长" + seed.specialty() + "等疾病的规范诊疗。");
            result.add(doctorRepository.save(doctor));
        }
        return result;
    }

    private List<Patient> ensurePatients() {
        List<PatientSeed> seeds = List.of(
                new PatientSeed("demo-openid-gaosi", "patient01", "高思晗", "女", 22, "13800001001", "无已知药物过敏史"),
                new PatientSeed("demo-openid-hanjie", "patient02", "韩杰", "男", 75, "13800001002", "青霉素过敏"),
                new PatientSeed("demo-openid-zhaoqing", "patient03", "赵清", "女", 36, "13800001003", "无已知药物过敏史"),
                new PatientSeed("demo-openid-zhengxing", "patient04", "郑行", "男", 41, "13800001004", "头孢类药物慎用")
        );
        List<Patient> result = new ArrayList<>();
        for (PatientSeed seed : seeds) {
            Patient patient = patientRepository.findByOpenid(seed.openid())
                    .or(() -> patientRepository.findByLoginAccount(seed.account()))
                    .or(() -> patientRepository.findByPhone(seed.phone()))
                    .orElseGet(Patient::new);
            patient.setOpenid(seed.openid());
            patient.setLoginAccount(seed.account());
            patient.setPasswordHash("123456");
            patient.setName(seed.name());
            patient.setGender(seed.gender());
            patient.setAge(seed.age());
            patient.setPhone(seed.phone());
            patient.setAllergyHistory(seed.allergy());
            patient.setAddress("大连市软件园云脑社区");
            result.add(patientRepository.save(patient));
        }
        return result;
    }

    private List<Medicine> ensureMedicines() {
        if (medicineRepository.count() > 0) {
            return medicineRepository.findAll();
        }
        List<MedicineSeed> seeds = List.of(
                new MedicineSeed("阿奇霉素片", "抗菌药", "0.25g*6片", "盒", "辰欣药业", 38.00, 120),
                new MedicineSeed("布洛芬缓释胶囊", "解热镇痛", "0.3g*20粒", "盒", "中美史克", 28.50, 95),
                new MedicineSeed("对乙酰氨基酚片", "解热镇痛", "0.5g*20片", "盒", "华润三九", 16.80, 180),
                new MedicineSeed("盐酸氨溴索片", "呼吸系统", "30mg*20片", "盒", "扬子江药业", 22.60, 86),
                new MedicineSeed("阿莫西林胶囊", "抗菌药", "0.5g*24粒", "盒", "石药集团", 25.00, 74),
                new MedicineSeed("氯雷他定片", "抗过敏", "10mg*12片", "盒", "拜耳医药", 19.90, 40),
                new MedicineSeed("银杏叶提取物片", "心脑血管", "40mg*30片", "盒", "康恩贝", 42.00, 70),
                new MedicineSeed("奥美拉唑肠溶胶囊", "消化系统", "20mg*14粒", "盒", "阿斯利康", 32.80, 65)
        );
        List<Medicine> result = new ArrayList<>();
        for (MedicineSeed seed : seeds) {
            Medicine m = new Medicine();
            m.setName(seed.name());
            m.setCategoryName(seed.category());
            m.setSpecification(seed.specification());
            m.setUnit(seed.unit());
            m.setManufacturer(seed.manufacturer());
            m.setPrice(BigDecimal.valueOf(seed.price()));
            m.setStock(seed.stock());
            m.setDescription(seed.category() + "常用药品，请遵医嘱使用。");
            result.add(medicineRepository.save(m));
        }
        return result;
    }

    private void ensureExaminationItems() {
        if (examinationItemRepository.count() > 0) return;
        List<ExaminationSeed> seeds = List.of(
                new ExaminationSeed("血常规", "检验", 35.00, "白细胞、红细胞、血红蛋白、血小板等基础血液指标"),
                new ExaminationSeed("尿常规", "检验", 25.00, "尿蛋白、尿糖、尿酮体、尿潜血等指标"),
                new ExaminationSeed("C反应蛋白", "检验", 48.00, "炎症感染辅助判断指标"),
                new ExaminationSeed("肝功能", "检验", 86.00, "转氨酶、胆红素、白蛋白等肝脏功能指标"),
                new ExaminationSeed("肾功能", "检验", 78.00, "肌酐、尿素氮、尿酸等指标"),
                new ExaminationSeed("胸部X线", "检查", 90.00, "用于肺部感染、胸痛等影像学筛查"),
                new ExaminationSeed("心电图", "检查", 45.00, "用于心律失常、胸闷胸痛筛查"),
                new ExaminationSeed("腹部彩超", "检查", 120.00, "用于肝胆胰脾肾等腹部器官筛查")
        );
        for (ExaminationSeed seed : seeds) {
            ExaminationItem item = new ExaminationItem();
            item.setName(seed.name());
            item.setType(seed.type());
            item.setPrice(BigDecimal.valueOf(seed.price()));
            item.setDescription(seed.description());
            examinationItemRepository.save(item);
        }
    }

    private List<Registration> ensureRegistrations(List<Doctor> doctors, List<Patient> patients, Map<String, Department> departments) {
        if (registrationRepository.count() > 0) return registrationRepository.findAll();
        List<Registration> result = new ArrayList<>();
        LocalDate today = LocalDate.now();
        result.add(saveRegistration(patients.get(0), doctors.get(0), departments.get("心血管内科"), today, "上午", "就诊中"));
        result.add(saveRegistration(patients.get(1), doctors.get(0), departments.get("心血管内科"), today, "下午", "待就诊"));
        result.add(saveRegistration(patients.get(2), doctors.get(2), departments.get("呼吸内科"), today.minusDays(1), "上午", "已就诊"));
        result.add(saveRegistration(patients.get(3), doctors.get(3), departments.get("儿科"), today.minusDays(2), "下午", "已就诊"));
        result.add(saveRegistration(patients.get(0), doctors.get(4), departments.get("骨科"), today.plusDays(1), "上午", "待就诊"));
        return result;
    }

    private Registration saveRegistration(Patient p, Doctor d, Department dep, LocalDate date, String slot, String status) {
        Registration r = new Registration();
        r.setPatientId(p.getId());
        r.setPatientName(p.getName());
        r.setDoctorId(d.getId());
        r.setDoctorName(d.getName());
        r.setDepartmentId(dep == null ? d.getDepartmentId() : dep.getId());
        r.setDepartmentName(dep == null ? d.getDepartmentName() : dep.getName());
        r.setRegistrationDate(date);
        r.setTimeSlot(slot);
        r.setStatus(status);
        return registrationRepository.save(r);
    }

    private void ensureMedicalRecords(List<Registration> registrations) {
        if (medicalRecordRepository.count() > 0 || registrations.isEmpty()) return;
        for (Registration r : registrations.stream().filter(item -> !"待就诊".equals(item.getStatus())).limit(3).toList()) {
            MedicalRecord mr = new MedicalRecord();
            mr.setRegistrationId(r.getId());
            mr.setPatientId(r.getPatientId());
            mr.setPatientName(r.getPatientName());
            mr.setDoctorId(r.getDoctorId());
            mr.setDoctorName(r.getDoctorName());
            mr.setDepartmentId(r.getDepartmentId());
            mr.setChiefComplaint("咳嗽、咽痛或胸闷不适 2-3 天");
            mr.setPresentIllness("患者近期出现相关症状，无明显意识障碍，精神一般，饮食睡眠稍受影响。");
            mr.setPastHistory("既往体健，否认重大慢性病史。特殊过敏史以患者档案为准。");
            mr.setPhysicalExamination("体温 37.2℃，心率 82 次/分，呼吸平稳，咽部轻度充血。心肺听诊未闻及明显异常。");
            mr.setDiagnosis("上呼吸道感染；待排除细菌感染");
            mr.setTreatment("建议多饮水、休息，遵医嘱用药；如高热不退或症状加重及时复诊。");
            medicalRecordRepository.save(mr);
        }
    }

    private void ensurePrescriptions(List<Registration> registrations, List<Medicine> medicines) {
        if (prescriptionRepository.count() > 0 || registrations.isEmpty() || medicines.size() < 3) return;
        Registration r1 = registrations.get(0);
        savePrescription(r1, List.of(drug(medicines.get(0), 1, "口服，一次1片，一日1次，连续3日"), drug(medicines.get(3), 1, "口服，一次1片，一日3次")));
        Registration r2 = registrations.size() > 2 ? registrations.get(2) : registrations.get(0);
        savePrescription(r2, List.of(drug(medicines.get(1), 1, "口服，一次1粒，一日2次，饭后服用"), drug(medicines.get(5), 1, "口服，一次1片，每晚1次")));
    }

    private void savePrescription(Registration r, List<Map<String, Object>> drugs) {
        BigDecimal total = drugs.stream()
                .map(item -> BigDecimal.valueOf(Double.parseDouble(String.valueOf(item.get("price")))).multiply(BigDecimal.valueOf(Long.parseLong(String.valueOf(item.get("quantity"))))))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Prescription p = new Prescription();
        p.setRegistrationId(r.getId());
        p.setPatientId(r.getPatientId());
        p.setPatientName(r.getPatientName());
        p.setDoctorId(r.getDoctorId());
        p.setDoctorName(r.getDoctorName());
        p.setDepartmentId(r.getDepartmentId());
        p.setDrugs(JSONUtil.toJsonStr(drugs));
        p.setTotalAmount(total);
        p.setStatus("待发药");
        prescriptionRepository.save(p);
    }


    /**
     * 纠偏医生端演示数据：旧库里可能残留“张子/泌尿外科”等早期测试数据。
     * 医生端联调默认使用 doctor01 / 123456，因此这里强制保持 doctor01 与其挂号记录一致。
     */
    private void normalizeDoctorDemoData(List<Doctor> doctors, List<Patient> patients, Map<String, Department> departments) {
        Doctor mainDoctor = doctors.stream()
                .filter(d -> "doctor01".equals(d.getUsername()))
                .findFirst()
                .orElse(null);
        Department mainDepartment = departments.get("心血管内科");
        if (mainDoctor == null || mainDepartment == null) return;

        mainDoctor.setName("张建国");
        mainDoctor.setTitle("主任医师");
        mainDoctor.setDepartmentId(mainDepartment.getId());
        mainDoctor.setDepartmentName(mainDepartment.getName());
        mainDoctor.setSpecialty("高血压、冠心病、心律失常、心力衰竭");
        mainDoctor.setIntroduction("从事心血管内科临床工作二十余年，擅长高血压、冠心病、心律失常及心力衰竭的规范化诊疗。");
        doctorRepository.save(mainDoctor);

        for (Registration registration : registrationRepository.findAll()) {
            if (Objects.equals(registration.getDoctorId(), mainDoctor.getId())) {
                registration.setDoctorName(mainDoctor.getName());
                registration.setDepartmentId(mainDepartment.getId());
                registration.setDepartmentName(mainDepartment.getName());
                registrationRepository.save(registration);
            }
        }

        LocalDate today = LocalDate.now();
        List<Registration> todayList = registrationRepository.findByDoctorIdAndRegistrationDateOrderByCreateTimeAsc(mainDoctor.getId(), today);
        if (todayList.size() < 3 && !patients.isEmpty()) {
            Set<Long> usedPatientIds = todayList.stream().map(Registration::getPatientId).collect(Collectors.toSet());
            List<String> slots = List.of("上午", "下午", "上午");
            List<String> statuses = List.of("就诊中", "待就诊", "待就诊");
            int index = 0;
            for (Patient patient : patients) {
                if (todayList.size() >= 3) break;
                if (usedPatientIds.contains(patient.getId())) continue;
                Registration created = saveRegistration(patient, mainDoctor, mainDepartment, today, slots.get(index % slots.size()), statuses.get(index % statuses.size()));
                todayList.add(created);
                usedPatientIds.add(patient.getId());
                index++;
            }
        }

        List<Registration> refreshedToday = registrationRepository.findByDoctorIdAndRegistrationDateOrderByCreateTimeAsc(mainDoctor.getId(), today);
        if (refreshedToday.stream().noneMatch(r -> "就诊中".equals(r.getStatus())) && !refreshedToday.isEmpty()) {
            Registration first = refreshedToday.get(0);
            first.setStatus("就诊中");
            registrationRepository.save(first);
        }
    }
    private Map<String, Object> drug(Medicine m, int quantity, String dosage) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("medicineId", m.getId());
        item.put("medicineName", m.getName());
        item.put("specification", m.getSpecification());
        item.put("quantity", quantity);
        item.put("unit", m.getUnit());
        item.put("dosage", dosage);
        item.put("price", m.getPrice());
        return item;
    }

    private record DoctorSeed(String username, String name, String title, String departmentName, String specialty) {}
    private record PatientSeed(String openid, String account, String name, String gender, Integer age, String phone, String allergy) {}
    private record MedicineSeed(String name, String category, String specification, String unit, String manufacturer, Double price, Integer stock) {}
    private record ExaminationSeed(String name, String type, Double price, String description) {}
}

