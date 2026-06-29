package com.neusoft.cloud_brain_diagnosis.service.impl;

import com.neusoft.cloud_brain_diagnosis.common.enums.RoleEnum;
import com.neusoft.cloud_brain_diagnosis.common.exception.BusinessException;
import com.neusoft.cloud_brain_diagnosis.common.util.JwtUtil;
import com.neusoft.cloud_brain_diagnosis.config.WechatConfig;
import com.neusoft.cloud_brain_diagnosis.entity.Patient;
import com.neusoft.cloud_brain_diagnosis.repository.PatientRepository;
import com.neusoft.cloud_brain_diagnosis.service.PatientService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PatientServiceImpl implements PatientService {
    private final PatientRepository patientRepository;
    private final WechatConfig wechatConfig;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

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
            String result = cn.hutool.http.HttpUtil.get(url, params);
            cn.hutool.json.JSONObject json = cn.hutool.json.JSONUtil.parseObj(result);
            if (json.getStr("errcode") != null) {
                throw new BusinessException("微信授权失败：" + json.getStr("errmsg"));
            }
            openid = json.getStr("openid");
            if (openid == null || openid.isBlank()) {
                throw new BusinessException("微信授权失败：未获取到openid");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("微信登录失败");
        }

        Patient patient = patientRepository.findByOpenid(openid).orElse(null);
        if (patient == null) {
            patient = new Patient();
            patient.setOpenid(openid);
            patient.setName("微信用户");
            patient = patientRepository.save(patient);
        }

        return buildLoginResult(patient);
    }

    @Override
    public Map<String, Object> testLogin(String account, String password) {
        String normalizedAccount = account == null ? "" : account.trim();
        if (normalizedAccount.isEmpty() || password == null || password.isBlank()) {
            throw new BusinessException("请输入测试账号和密码");
        }
        Patient patient = patientRepository.findByLoginAccount(normalizedAccount)
                .orElseThrow(() -> new BusinessException("测试账号或密码错误"));
        if (patient.getPasswordHash() == null || !passwordEncoder.matches(password, patient.getPasswordHash())) {
            throw new BusinessException("测试账号或密码错误");
        }
        return buildLoginResult(patient);
    }

    private Map<String, Object> buildLoginResult(Patient patient) {
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("token", jwtUtil.generateToken(patient.getId(), RoleEnum.PATIENT.getCode()));
        resultMap.put("patientId", patient.getId());
        resultMap.put("name", patient.getName());
        resultMap.put("phone", patient.getPhone());
        resultMap.put("needCompleteInfo", patient.getPhone() == null);
        return resultMap;
    }

    /**
     * 实训演示环境兜底：
     * 微信开发者工具里如果 AppID、AppSecret 或网络暂时不可用，真实 jscode2session 会失败。
     * 为了不让患者端授权页反复闪回，这里降级成一个固定的本地模拟微信身份。
     * 生产环境只需要配置正确的 wechat.appid / wechat.secret，真实微信登录成功后不会走到这里。
     */
    private String buildLocalDemoOpenid() {
        return "local-demo-wechat-patient";
    }

    @Override
    public Patient getPatientInfo(Long id) {
        return patientRepository.findById(id)
                .orElseThrow(() -> new BusinessException("患者不存在"));
    }

    @Override
    public String updatePatientInfo(Patient updatePatient) {
        Patient patient = patientRepository.findById(updatePatient.getId())
                .orElseThrow(() -> new BusinessException("患者不存在"));
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
            throw new BusinessException("该手机号已被绑定");
        }
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new BusinessException("患者不存在"));
        patient.setPhone(phone);
        patientRepository.save(patient);
        return "手机号绑定成功";
    }
}
