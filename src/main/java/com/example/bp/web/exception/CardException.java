package com.example.bp.web.exception;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * HTMX 폼 제출 처리 중 발생한, "해당 폼 카드 fragment를 오류 토스트와 함께 다시 그려야 하는" 오류.
 *
 * 다시 그릴 fragment 이름을 필수로 받고, 폼 상태 복원용 모델 속성(with)과 HX-Trigger 헤더
 * (hxTrigger)는 선택적으로 담는다. 컨트롤러는 오류 지점에서 throw 만 하면 되고, 렌더링은
 * GlobalExceptionHandler가 한곳에서 처리한다.
 *
 * 무상태 카드:  throw new CardException("client/password :: card", "현재 비밀번호가 올바르지 않습니다.");
 * 상태 보존 카드: throw new CardException("home/contact :: card", msg).with("email", email).hxTrigger("turnstileReset");
 */
public class CardException extends RuntimeException {

    private final String fragment;
    private String hxTrigger;
    private final Map<String, Object> model = new LinkedHashMap<>();

    public CardException(String fragment, String message) {
        super(message);
        this.fragment = fragment;
    }

    /** HX-Trigger 헤더(예: "turnstileReset")를 함께 보낸다. */
    public CardException hxTrigger(String hxTrigger) {
        this.hxTrigger = hxTrigger;
        return this;
    }

    /** 카드 재렌더에 필요한 모델 속성(폼 입력값 복원 등)을 추가한다. */
    public CardException with(String key, Object value) {
        model.put(key, value);
        return this;
    }

    public String fragment() {
        return fragment;
    }

    public String hxTrigger() {
        return hxTrigger;
    }

    public Map<String, Object> model() {
        return model;
    }
}
