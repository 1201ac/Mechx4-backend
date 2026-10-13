package com.nerlogix;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class NerlogixApplication {
    public static void main(String[] args) {
        loadDotEnv();
        SpringApplication.run(NerlogixApplication.class, args);
    }

    private static void loadDotEnv() {
        java.nio.file.Path[] candidates = new java.nio.file.Path[] {
            java.nio.file.Paths.get(".env"),
            java.nio.file.Paths.get("backend", ".env"),
            java.nio.file.Paths.get("..", ".env")
        };
        for (java.nio.file.Path p : candidates) {
            if (java.nio.file.Files.exists(p)) {
                try {
                    for (String line : java.nio.file.Files.readAllLines(p)) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#")) continue;
                        int idx = line.indexOf('=');
                        if (idx > 0) {
                            String key = line.substring(0, idx).trim();
                            String val = line.substring(idx + 1).trim();
                            if (System.getProperty(key) == null && System.getenv(key) == null) {
                                System.setProperty(key, val);
                            }
                        }
                    }
                    break;
                } catch (Exception ignored) {}
            }
        }
    }
}
