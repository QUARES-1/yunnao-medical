package com.neusoft.cloud_brain_diagnosis.repository;

import com.neusoft.cloud_brain_diagnosis.entity.Registration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface RegistrationRepository extends JpaRepository<Registration, Long> {
    Page<Registration> findByPatientIdOrderByCreateTimeDesc(Long patientId, Pageable pageable);
    Page<Registration> findByPatientIdAndStatusOrderByCreateTimeDesc(Long patientId, String status, Pageable pageable);
    List<Registration> findByDoctorIdAndRegistrationDateOrderByCreateTimeAsc(Long doctorId, LocalDate date);
    Registration findTopByPatientIdAndDoctorIdAndRegistrationDateAndTimeSlotAndStatusNotOrderByCreateTimeDesc(
            Long patientId,
            Long doctorId,
            LocalDate registrationDate,
            String timeSlot,
            String status);
    Page<Registration> findByDoctorIdOrderByCreateTimeDesc(Long doctorId, Pageable pageable);
    @Query("""
            select r from Registration r
            where (:doctorId is null or r.doctorId = :doctorId)
              and (:status is null or :status = '' or r.status = :status)
              and (
                    :keyword is null or :keyword = ''
                    or r.patientName like concat('%', :keyword, '%')
                    or r.doctorName like concat('%', :keyword, '%')
                    or r.departmentName like concat('%', :keyword, '%')
              )
            """)
    Page<Registration> searchDoctorHistory(
            @Param("doctorId") Long doctorId,
            @Param("keyword") String keyword,
            @Param("status") String status,
            Pageable pageable);
    long countByDoctorIdAndRegistrationDateAndTimeSlot(Long doctorId, LocalDate date, String timeSlot);
    long countByDoctorIdAndRegistrationDateAndTimeSlotAndStatusNot(
            Long doctorId, LocalDate date, String timeSlot, String status);
}
