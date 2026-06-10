package com.example.bp.web.home;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 공개 법적 고지 페이지 (FR-4): 저장된 {@code settings.privacy} /
 * {@code settings.terms} HTML을 렌더링한다 (관리자가 작성하며 {@code th:utext}로 렌더링).
 * {@code setting} 모델 속성은 GlobalModelAttributes에서 제공된다.
 */
@Controller
public class LegalController {

    @GetMapping("/privacy")
    public String privacy() {
        return "home/privacy";
    }

    @GetMapping("/terms")
    public String terms() {
        return "home/terms";
    }
}
