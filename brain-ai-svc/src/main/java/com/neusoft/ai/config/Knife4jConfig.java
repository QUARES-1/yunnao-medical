package com.neusoft.ai.config;

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
                        .title("云脑诊疗平台 - AI能力微服务")
                        .description("AI能力层独立微服务接口文档")
                        .version("1.0.0"));
    }
}
