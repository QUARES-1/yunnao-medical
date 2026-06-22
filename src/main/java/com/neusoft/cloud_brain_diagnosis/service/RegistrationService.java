package com.neusoft.cloud_brain_diagnosis.service;

import com.neusoft.cloud_brain_diagnosis.entity.Registration;
import org.springframework.data.domain.Page;
import java.util.List;

public interface RegistrationService {
    Registration createRegistration(Registration registration);
    Page<Registration> getPatientRegistrationList(Long patientId, String status, Integer page, Integer size);
    Registration getDetail(Long id, Long userId, String role);
    String cancelRegistration(Long id, Long patientId);
    List<Registration> getDoctorTodayList(Long doctorId);
    Page<Registration> getDoctorList(Long doctorId, String keyword, String status, Integer page, Integer size);
    String startConsultation(Long id, Long doctorId);
    String completeConsultation(Long id, Long doctorId);
}
