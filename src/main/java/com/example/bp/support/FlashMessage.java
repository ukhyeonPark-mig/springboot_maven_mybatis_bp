package com.example.bp.support;

import jakarta.servlet.http.HttpSession;

/**
 * 세션에 저장되는 일회성 flash 메시지로, 다음 요청에서 {@code GlobalModelAttributes}에
 * 의해 toast 모델 속성({@code message}/{@code success}/{@code error})으로 노출된다.
 * HTMX {@code HX-Redirect}가 toast를 보여줘야 하는 새 페이지로 이동할 때 사용된다
 * (PRD §8.2/§15.2).
 */
public final class FlashMessage {

    public static final String SUCCESS = "flash.success";
    public static final String ERROR = "flash.error";
    public static final String MESSAGE = "flash.message";

    private FlashMessage() {
    }

    public static void success(HttpSession session, String text) {
        session.setAttribute(SUCCESS, text);
    }

    public static void error(HttpSession session, String text) {
        session.setAttribute(ERROR, text);
    }

    public static void message(HttpSession session, String text) {
        session.setAttribute(MESSAGE, text);
    }
}
