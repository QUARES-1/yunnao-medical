package com.neusoft.cloud_brain_diagnosis.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "staff_account")
@Data
public class StaffAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 30)
    private String name;

    @Column(nullable = false, length = 20)
    private String role;

    private String phone;
    private Boolean enabled = true;
    private LocalDateTime createTime;

    @PrePersist
    void onCreate() {
        if (createTime == null) createTime = LocalDateTime.now();
        if (enabled == null) enabled = true;
    }
}
