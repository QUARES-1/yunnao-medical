package com.neusoft.cloud_brain_diagnosis.service;

import com.neusoft.cloud_brain_diagnosis.entity.Patient;
import java.util.Map;

public interface PatientService {
    Map<String, Object> wxLogin(String code);
    Map<String, Object> testLogin(String account, String password);
    Patient getPatientInfo(Long id);
    String updatePatientInfo(Patient patient);
    String bindPhone(Long patientId, String phone);
}
