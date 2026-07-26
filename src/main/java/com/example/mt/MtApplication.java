package com.example.mt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class MtApplication {
    public static void main(String[] args) {
        SpringApplication.run(MtApplication.class, args);
    }
}
