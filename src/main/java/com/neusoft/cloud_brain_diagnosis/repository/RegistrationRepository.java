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
    Page<Registration> findByDoctorIdOrderByCreateTimeDesc(Long doctorId, Pageable pageable);
    @Query("SELECT r FROM Registration r WHERE r.doctorId = :doctorId " +
            "AND r.status IN ('已就诊', '已取消') " +
            "AND (:status IS NULL OR :status = '' OR r.status = :status) " +
            "AND (:keyword IS NULL OR :keyword = '' OR r.patientName LIKE CONCAT('%', :keyword, '%'))")
    Page<Registration> searchDoctorHistory(@Param("doctorId") Long doctorId,
                                           @Param("keyword") String keyword,
                                           @Param("status") String status,
                                           Pageable pageable);
    long countByDoctorIdAndRegistrationDateAndTimeSlot(Long doctorId, LocalDate date, String timeSlot);
}
