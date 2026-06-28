package com.neusoft.cloud_brain_diagnosis.controller;

import com.neusoft.cloud_brain_diagnosis.common.enums.RoleEnum;
import com.neusoft.cloud_brain_diagnosis.common.result.Result;
import com.neusoft.cloud_brain_diagnosis.common.util.JwtUtil;
import com.neusoft.cloud_brain_diagnosis.entity.Patient;
import com.neusoft.cloud_brain_diagnosis.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/** 仅供本地游客 AppID 联调，正式环境不会加载。 */
@Profile("dev")
@RestController
@RequestMapping("/api/dev")
@RequiredArgsConstructor
public class DevPatientController {
    private final PatientRepository patientRepository;
    private final JwtUtil jwtUtil;

    @PostMapping("/patient-login")
    public Result<Map<String, Object>> patientLogin() {
        Patient patient = patientRepository.findByOpenid("dev-tourist-patient").orElseGet(() -> {
            Patient created = new Patient();
            created.setOpenid("dev-tourist-patient");
            created.setName("高同学");
            created.setPhone("13800002026");
            created.setGender("女");
            created.setAge(20);
            return patientRepository.save(created);
        });

        Map<String, Object> data = new HashMap<>();
        data.put("token", jwtUtil.generateToken(patient.getId(), RoleEnum.PATIENT.getCode()));
        data.put("patientId", patient.getId());
        data.put("name", patient.getName());
        data.put("phone", patient.getPhone());
        data.put("needCompleteInfo", false);
        return Result.success(data);
    }
}
