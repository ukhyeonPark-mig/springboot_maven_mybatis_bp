package com.example.bp.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis mapper 스캐닝. 웹 슬라이스 테스트({@code @WebMvcTest})가 DataSource 없이
 * mapper를 연결하려 시도하지 않도록 main 클래스와 분리해 둔다.
 */
@Configuration
@MapperScan("com.example.bp.mapper")
public class MyBatisConfig {
}
