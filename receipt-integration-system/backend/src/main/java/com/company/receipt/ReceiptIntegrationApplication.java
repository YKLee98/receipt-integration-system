package com.company.receipt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableScheduling
@EnableCaching
@EnableRetry
public class ReceiptIntegrationApplication {
    public static void main(String[] args) {
        SpringApplication.run(ReceiptIntegrationApplication.class, args);
    }
}