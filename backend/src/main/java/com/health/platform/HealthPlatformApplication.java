package com.health.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.health")
public class HealthPlatformApplication {
    public static void main(String[] args) {
        SpringApplication.run(HealthPlatformApplication.class, args);
    }
}
