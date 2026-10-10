package com.nerlogix;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class NerlogixApplication {
    public static void main(String[] args) {
        SpringApplication.run(NerlogixApplication.class, args);
    }
}
