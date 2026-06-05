package com.example.bp.config;

import nz.net.ultraq.thymeleaf.layoutdialect.LayoutDialect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * thymeleaf-layout-dialect ships no Spring Boot auto-config, so the dialect
 * bean must be registered manually to enable {@code layout:decorate}/{@code layout:fragment}
 * (PRD §3.3). The Spring Security dialect is auto-configured by Boot.
 */
@Configuration
public class ThymeleafConfig {

    @Bean
    public LayoutDialect layoutDialect() {
        return new LayoutDialect();
    }
}
