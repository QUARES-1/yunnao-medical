package com.neusoft.cloud_brain_diagnosis;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 云脑诊疗平台 应用启动测试
 * 验证Spring上下文能正常加载所有Bean
 */
@SpringBootTest
@ActiveProfiles("test")
class CloudBrainDiagnosisApplicationTests {

    @Test
    void contextLoads() {
        // 验证Spring上下文加载成功
    }

    @Test
    void mainMethodRuns() {
        // 验证主启动类main方法能正常调用（不抛出异常）
        CloudBrainDiagnosisApplication.main(new String[]{});
    }
}
