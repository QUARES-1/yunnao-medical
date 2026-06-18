package com.neusoft.cloud_brain_diagnosis.repository;

import com.neusoft.cloud_brain_diagnosis.entity.MedicalRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, Long> {
    Optional<MedicalRecord> findByRegistrationId(Long registrationId);
    Page<MedicalRecord> findByPatientIdOrderByCreateTimeDesc(Long patientId, Pageable pageable);
    Page<MedicalRecord> findByDoctorIdOrderByCreateTimeDesc(Long doctorId, Pageable pageable);
}