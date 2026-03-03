package com.futsite;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FutsiteApplication {
    public static void main(String[] args) {
        SpringApplication.run(FutsiteApplication.class, args);
    }
}
