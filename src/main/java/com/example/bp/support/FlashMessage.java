package com.example.bp.support;

import jakarta.servlet.http.HttpSession;

/**
 * One-shot flash messages stored in the session, surfaced as the toast model
 * attributes ({@code message}/{@code success}/{@code error}) on the next request
 * by {@code GlobalModelAttributes}. Used when an HTMX {@code HX-Redirect} navigates
 * to a fresh page that should show a toast (PRD §8.2/§15.2).
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
