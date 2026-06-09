package com.example.bp.config;

import nz.net.ultraq.thymeleaf.layoutdialect.LayoutDialect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Thymeleaf 본체는 Spring Boot가 자동 구성하지만, 페이지를 공통 레이아웃에 끼워 넣는
 * 레이아웃 상속 기능(layout:decorate)을 주는 서드파티 애드온 thymeleaf-layout-dialect는
 * 자동 구성 대상이 아니다 (Spring Boot 2.0부터 제외됨). 따라서 {@code layout:decorate} /
 * {@code layout:fragment}를 쓰려면 이 LayoutDialect 빈을 직접 등록해야 한다 (PRD §3.3).
 *
 * 이 빈이 없으면 layout:* 속성이 무시되어 모든 페이지가 공통 레이아웃 없이 렌더된다.
 * 참고: sec:authorize 같은 Spring Security dialect는 Boot가 자동 구성하므로 등록이 필요 없다.
 */
@Configuration
public class ThymeleafConfig {

    @Bean
    public LayoutDialect layoutDialect() {
        return new LayoutDialect();
    }
}
