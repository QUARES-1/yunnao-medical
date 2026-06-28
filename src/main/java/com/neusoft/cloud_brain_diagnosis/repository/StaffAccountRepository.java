package com.neusoft.cloud_brain_diagnosis.repository;

import com.neusoft.cloud_brain_diagnosis.entity.StaffAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface StaffAccountRepository extends JpaRepository<StaffAccount, Long> {
    Optional<StaffAccount> findByUsername(String username);
    boolean existsByUsername(String username);
}
