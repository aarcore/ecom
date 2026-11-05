package com.ait;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Single deployable: the back-office API and the customer API live in one
 * Spring context so both clients share the same transactional services.
 */
@EnableAsync
@EnableScheduling
@SpringBootApplication(scanBasePackages = "com.ait")
public class EcomApplication {

    public static void main(String[] args) {
        SpringApplication.run(EcomApplication.class, args);
    }
}
