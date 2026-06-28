package com.neusoft.cloud_brain_diagnosis.repository;

import com.neusoft.cloud_brain_diagnosis.entity.Examination;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExaminationRepository extends JpaRepository<Examination, Long> {
    Page<Examination> findByPatientIdOrderByCreateTimeDesc(Long patientId, Pageable pageable);
    Page<Examination> findByDoctorIdOrderByCreateTimeDesc(Long doctorId, Pageable pageable);
    Page<Examination> findByStatusOrderByCreateTimeDesc(String status, Pageable pageable);
    Page<Examination> findAllByOrderByCreateTimeDesc(Pageable pageable);
    List<Examination> findByRegistrationIdOrderByCreateTimeDesc(Long registrationId);
    boolean existsByRegistrationIdAndItemIdAndStatus(Long registrationId, Long itemId, String status);
}
