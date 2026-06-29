package com.neusoft.cloud_brain_diagnosis.repository;

import com.neusoft.cloud_brain_diagnosis.entity.Registration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface RegistrationRepository extends JpaRepository<Registration, Long> {
    Page<Registration> findByPatientIdOrderByCreateTimeDesc(Long patientId, Pageable pageable);
    Page<Registration> findByPatientIdAndStatusOrderByCreateTimeDesc(Long patientId, String status, Pageable pageable);
    List<Registration> findByDoctorIdAndRegistrationDateOrderByCreateTimeAsc(Long doctorId, LocalDate date);
    Page<Registration> findByDoctorIdOrderByCreateTimeDesc(Long doctorId, Pageable pageable);
    long countByDoctorIdAndRegistrationDateAndTimeSlot(Long doctorId, LocalDate date, String timeSlot);
    long countByDoctorIdAndRegistrationDateAndTimeSlotAndStatusNot(
            Long doctorId, LocalDate date, String timeSlot, String status);
}
