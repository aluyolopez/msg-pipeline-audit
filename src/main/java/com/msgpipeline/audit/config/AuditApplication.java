package com.msgpipeline.audit.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Punto de entrada Spring Boot para el perfil 'local'.
 * En Lambda, el entry point es AuditHandler.java.
 */
@SpringBootApplication(scanBasePackages = "com.msgpipeline.audit")
@EnableConfigurationProperties(AppConfig.class)
public class AuditApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuditApplication.class, args);
    }
}
