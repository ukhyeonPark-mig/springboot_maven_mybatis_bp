package com.example.bp.config;

import nz.net.ultraq.thymeleaf.layoutdialect.LayoutDialect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * thymeleaf-layout-dialect에는 Spring Boot 자동 구성이 없으므로,
 * {@code layout:decorate}/{@code layout:fragment}를 사용하려면 dialect bean을
 * 수동으로 등록해야 한다 (PRD §3.3). Spring Security dialect는 Boot가 자동 구성한다.
 */
@Configuration
public class ThymeleafConfig {

    @Bean
    public LayoutDialect layoutDialect() {
        return new LayoutDialect();
    }
}
