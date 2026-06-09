package com.example.bp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * {@code @Async}를 활성화한다 (PRD §10). 메일 발송은 현재 동기 방식이라
 * contact/OTP 흐름에서 성공/실패를 인라인으로 바로 노출할 수 있다. 무거운 작업이나
 * 재시도가 잦은 작업은 추후 {@code @Async} 메서드나 job 테이블로 옮길 수 있다.
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}
