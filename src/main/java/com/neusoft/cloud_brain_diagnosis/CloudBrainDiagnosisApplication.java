package com.neusoft.cloud_brain_diagnosis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 云脑诊疗平台启动类
 *
 * 注意：这个类必须放在 com.neusoft.cloud_brain_diagnosis 包的最外层
 * 不能放在任何子包里面，否则扫描不到其他类
 */
@SpringBootApplication
public class CloudBrainDiagnosisApplication {

	public static void main(String[] args) {
		SpringApplication.run(CloudBrainDiagnosisApplication.class, args);
		System.out.println("============================================");
		System.out.println("  云脑诊疗平台后端启动成功！");
		System.out.println("  接口文档：http://localhost:8080/doc.html");
		System.out.println("============================================");
	}
}