package com.neusoft.cloud_brain_diagnosis.common.enums;

import lombok.Getter;

@Getter
public enum RoleEnum {
    PATIENT("patient", "患者"),
    DOCTOR("doctor", "医生"),
    ADMIN("admin", "管理员"),
    PHARMACY("pharmacy", "药房"),
    LAB("lab", "检验科");

    private final String code;
    private final String desc;

    RoleEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}