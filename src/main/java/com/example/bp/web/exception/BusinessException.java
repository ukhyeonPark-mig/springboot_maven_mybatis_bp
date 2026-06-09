package com.example.bp.web.exception;

import org.springframework.http.HttpStatus;

/**
 * 비즈니스 로직에서 "예상되는" 일반 오류를 나타내는 기본 예외.
 *
 * 특정 폼 카드 fragment를 다시 그려야 하는 경우에는 {@link CardException}을 쓰고,
 * 그 외의 일반 오류(폼과 무관한 처리 실패 등)에 이 예외를 쓴다.
 * GlobalExceptionHandler가 HTMX 요청이면 토스트로, 일반 요청이면 오류 페이지로 분기한다.
 */
public class BusinessException extends RuntimeException {

    private final HttpStatus status;

    public BusinessException(String message) {
        this(message, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    public BusinessException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}
