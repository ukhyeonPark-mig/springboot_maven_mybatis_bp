package com.example.bp.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis mapper scanning, kept separate from the main class so web-slice tests
 * ({@code @WebMvcTest}) don't try to wire mappers without a DataSource.
 */
@Configuration
@MapperScan("com.example.bp.mapper")
public class MyBatisConfig {
}
