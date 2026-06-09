package com.example.bp;

import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

import jakarta.annotation.PostConstruct;

@SpringBootApplication
@ConfigurationPropertiesScan
public class BpApplication {

    public static void main(String[] args) {
        SpringApplication.run(BpApplication.class, args);
    }

    /** 애플리케이션 전역 타임존 = Asia/Seoul (PRD §8.6). */
    @PostConstruct
    void init() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
    }
}
