package com.neusoft.cloud_brain_diagnosis.service;

import com.neusoft.cloud_brain_diagnosis.entity.Doctor;
import org.springframework.data.domain.Page;
import java.util.List;
import java.util.Map;

public interface DoctorService {
    String login(String username, String password);
    List<Doctor> getDoctorList(Long departmentId);
    Doctor getDoctorDetail(Long id);
    Doctor getDoctorInfo(Long id);
    String addDoctor(Doctor doctor);
    String updateDoctorInfo(Doctor doctor);
    Page<Doctor> getDoctorPage(Integer page, Integer size);
    String resetPassword(Long id);
    String deleteDoctor(Long id);
    Map<String, Object> getSchedule(Long doctorId);
    /**
     * 医生自助注册
     */
    String register(String username, String password, String name);
}