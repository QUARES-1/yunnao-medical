package com.neusoft.cloud_brain_diagnosis.repository;

import com.neusoft.cloud_brain_diagnosis.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {
    Optional<Patient> findByOpenid(String openid);
    Optional<Patient> findByLoginAccount(String loginAccount);
    Optional<Patient> findByPhone(String phone);
    boolean existsByOpenid(String openid);
    boolean existsByPhone(String phone);
}
