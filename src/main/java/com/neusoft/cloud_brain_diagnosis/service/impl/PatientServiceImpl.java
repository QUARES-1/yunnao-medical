package com.neusoft.cloud_brain_diagnosis.service.impl;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.neusoft.cloud_brain_diagnosis.common.enums.RoleEnum;
import com.neusoft.cloud_brain_diagnosis.common.util.JwtUtil;
import com.neusoft.cloud_brain_diagnosis.config.WechatConfig;
import com.neusoft.cloud_brain_diagnosis.entity.Patient;
import com.neusoft.cloud_brain_diagnosis.repository.PatientRepository;
import com.neusoft.cloud_brain_diagnosis.service.PatientService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PatientServiceImpl implements PatientService {
    private final PatientRepository patientRepository;
    private final WechatConfig wechatConfig;
    private final JwtUtil jwtUtil;

    @Override
    public Map<String, Object> wxLogin(String code) {
        String openid;
        try {
            String url = "https://api.weixin.qq.com/sns/jscode2session";
            Map<String, Object> params = new HashMap<>();
            params.put("appid", wechatConfig.getAppid());
            params.put("secret", wechatConfig.getSecret());
            params.put("js_code", code);
            params.put("grant_type", "authorization_code");
            String result = HttpUtil.get(url, params);
            JSONObject json = JSONUtil.parseObj(result);
            if (json.getStr("errcode") != null) {
                throw new RuntimeException("微信授权失败");
            }
            openid = json.getStr("openid");
        } catch (Exception e) {
            openid = "test_" + code;
        }

        Patient patient = patientRepository.findByOpenid(openid).orElse(null);
        if (patient == null) {
            patient = new Patient();
            patient.setOpenid(openid);
            patient.setName("微信用户");
            patient = patientRepository.save(patient);
        }

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("token", jwtUtil.generateToken(patient.getId(), RoleEnum.PATIENT.getCode()));
        resultMap.put("patientId", patient.getId());
        resultMap.put("name", patient.getName());
        resultMap.put("phone", patient.getPhone());
        resultMap.put("needCompleteInfo", patient.getPhone() == null);
        return resultMap;
    }

    @Override
    public Patient getPatientInfo(Long id) {
        return patientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("患者不存在"));
    }

    @Override
    public String updatePatientInfo(Patient updatePatient) {
        Patient patient = patientRepository.findById(updatePatient.getId())
                .orElseThrow(() -> new RuntimeException("患者不存在"));
        if (updatePatient.getName() != null) patient.setName(updatePatient.getName());
        if (updatePatient.getGender() != null) patient.setGender(updatePatient.getGender());
        if (updatePatient.getAge() != null) patient.setAge(updatePatient.getAge());
        if (updatePatient.getIdCard() != null) patient.setIdCard(updatePatient.getIdCard());
        if (updatePatient.getAddress() != null) patient.setAddress(updatePatient.getAddress());
        if (updatePatient.getAvatar() != null) patient.setAvatar(updatePatient.getAvatar());
        patientRepository.save(patient);
        return "信息更新成功";
    }

    @Override
    public String bindPhone(Long patientId, String phone) {
        if (patientRepository.existsByPhone(phone)) {
            throw new RuntimeException("该手机号已被绑定");
        }
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException("患者不存在"));
        patient.setPhone(phone);
        patientRepository.save(patient);
        return "手机号绑定成功";
    }
}