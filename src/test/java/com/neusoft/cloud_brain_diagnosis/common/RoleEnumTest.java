package com.neusoft.cloud_brain_diagnosis.common;

import com.neusoft.cloud_brain_diagnosis.common.enums.RoleEnum;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RoleEnum 角色枚举测试
 */
class RoleEnumTest {

    @Test
    void testAllRoles() {
        assertEquals("patient", RoleEnum.PATIENT.getCode());
        assertEquals("患者", RoleEnum.PATIENT.getDesc());

        assertEquals("doctor", RoleEnum.DOCTOR.getCode());
        assertEquals("医生", RoleEnum.DOCTOR.getDesc());

        assertEquals("admin", RoleEnum.ADMIN.getCode());
        assertEquals("管理员", RoleEnum.ADMIN.getDesc());

        assertEquals("pharmacy", RoleEnum.PHARMACY.getCode());
        assertEquals("药房", RoleEnum.PHARMACY.getDesc());

        assertEquals("lab", RoleEnum.LAB.getCode());
        assertEquals("检验科", RoleEnum.LAB.getDesc());
    }

    @Test
    void testCodeUniqueness() {
        // 所有角色的code应该唯一
        long uniqueCodes = java.util.Arrays.stream(RoleEnum.values())
                .map(RoleEnum::getCode)
                .distinct()
                .count();
        assertEquals(RoleEnum.values().length, uniqueCodes);
    }
}
