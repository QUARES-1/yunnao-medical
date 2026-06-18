package com.neusoft.cloud_brain_diagnosis.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Knife4jConfig {
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("云脑诊疗平台API文档")
                        .version("1.0.0")
                        .description("云脑诊疗平台后端接口文档"));
    }
}