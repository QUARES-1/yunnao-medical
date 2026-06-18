package com.neusoft.cloud_brain_diagnosis.config;

import com.neusoft.cloud_brain_diagnosis.config.interceptor.JwtInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {
    private final JwtInterceptor jwtInterceptor;

    @Value("${file.upload.path:D:/upload/}")
    private String uploadPath;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/api/patient/wx-login",
                        "/api/doctor/login",
                        "/api/admin/login",
                        "/api/department/list",
                        "/api/department/detail/**",
                        "/api/doctor/list",
                        "/api/doctor/detail/**",
                        "/api/doctor/*/schedule",
                        "/api/medicine/list",
                        "/api/medicine/detail/**",
                        "/api/medicine/category/list",
                        "/api/examination/item/list",
                        "/doc.html",
                        "/webjars/**",
                        "/v3/**",
                        "/swagger-resources/**",
                        "/swagger-ui/**",
                        "/favicon.ico",
                        "/upload/**"
                );
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/upload/**")
                .addResourceLocations("file:" + uploadPath);
    }
}