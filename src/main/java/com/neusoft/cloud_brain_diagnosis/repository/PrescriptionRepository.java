package com.neusoft.cloud_brain_diagnosis.repository;

import com.neusoft.cloud_brain_diagnosis.entity.Prescription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {
    Page<Prescription> findByPatientIdOrderByCreateTimeDesc(Long patientId, Pageable pageable);
    Page<Prescription> findByDoctorIdOrderByCreateTimeDesc(Long doctorId, Pageable pageable);
    Page<Prescription> findByStatusOrderByCreateTimeDesc(String status, Pageable pageable);
    Page<Prescription> findAllByOrderByCreateTimeDesc(Pageable pageable);
    List<Prescription> findByRegistrationIdOrderByCreateTimeDesc(Long registrationId);
    List<Prescription> findByRegistrationIdAndStatusNotOrderByCreateTimeDesc(Long registrationId, String status);
}
