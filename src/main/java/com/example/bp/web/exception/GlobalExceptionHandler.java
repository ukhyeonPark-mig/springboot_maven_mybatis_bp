package com.example.bp.web.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * 전역 예외 처리 (@ControllerAdvice). 비즈니스 로직은 오류 지점에서 throw 만 하고
 * 응답 생성을 이곳에 위임한다 — 컨트롤러에서 try-catch와 인라인 오류 렌더링이 사라진다.
 *
 * @RestControllerAdvice가 아니라 @ControllerAdvice를 쓰는 이유: 반환 String을 "응답 본문"이
 * 아니라 "뷰 이름"으로 해석시켜 Thymeleaf fragment/페이지를 렌더링하기 위함이다.
 *
 * HTMX 요청(HX-Request 헤더 존재)과 일반 요청을 구분해 분기한다:
 *   - HTMX  → 부분 fragment(폼 카드 또는 OOB 토스트)를 200으로 반환
 *   - 일반   → 전체 오류 페이지(error/*.html)
 *
 * 참고: SigninController는 폼 상태가 복잡해 자체 @ExceptionHandler(로컬)를 따로 두며,
 * 로컬 핸들러가 전역보다 우선하므로 여기와 충돌하지 않는다.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String OOB_ERROR = "fragments/message :: errorOob";

    /**
     * HTMX 폼 오류: 지정된 카드 fragment를 오류 토스트 + (있으면) 폼 상태와 함께 다시 그린다.
     * 폼 제출은 항상 HTMX이므로 별도 분기 없이 fragment를 렌더링한다.
     */
    @ExceptionHandler(CardException.class)
    public String handleCard(CardException ex, HttpServletResponse response, Model model) {
        if (ex.hxTrigger() != null) {
            response.setHeader("HX-Trigger", ex.hxTrigger());
        }
        ex.model().forEach(model::addAttribute);
        model.addAttribute("error", ex.getMessage());
        return ex.fragment();
    }

    /** 예상된 비즈니스 오류: HTMX면 OOB 토스트, 일반 요청이면 상태에 맞는 오류 페이지. */
    @ExceptionHandler(BusinessException.class)
    public String handleBusiness(BusinessException ex, HttpServletRequest request,
                                 HttpServletResponse response, Model model) {
        if (isHtmx(request)) {
            response.setHeader("HX-Reswap", "none"); // 트리거 요소는 교체하지 않고 토스트만 띄움
            model.addAttribute("error", ex.getMessage());
            return OOB_ERROR;
        }
        response.setStatus(ex.status().value());
        return errorView(ex.status().value());
    }

    /**
     * 예상치 못한 오류: HTMX면 일반 토스트로 표시(+로그), 일반 요청이면 다시 던져
     * Spring Boot의 기본 오류 처리(error/500.html 등)에 맡긴다.
     */
    @ExceptionHandler(Exception.class)
    public String handleUnexpected(Exception ex, HttpServletRequest request,
                                   HttpServletResponse response, Model model) throws Exception {
        log.error("처리되지 않은 예외", ex);
        if (!isHtmx(request)) {
            throw ex;
        }
        response.setHeader("HX-Reswap", "none");
        model.addAttribute("error", "요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.");
        return OOB_ERROR;
    }

    private boolean isHtmx(HttpServletRequest request) {
        return "true".equalsIgnoreCase(request.getHeader("HX-Request"));
    }

    private String errorView(int status) {
        return switch (status) {
            case 403, 404, 503 -> "error/" + status;
            default -> "error/500";
        };
    }
}
