package com.example.bp.web;

import com.example.bp.domain.Setting;
import com.example.bp.service.BrandingService;
import com.example.bp.service.R2StorageService;
import com.example.bp.service.SettingService;
import com.example.bp.support.AppProperties;
import com.example.bp.support.FlashMessage;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.core.env.Environment;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * 모든 뷰(레이아웃, 내비게이션 바, 사이드바, 푸터)에서 사용할 수 있는 공통 모델
 * 속성: 앱 이름, 설정 싱글톤, 활성 내비게이션용 현재 URI, HTMX 메타 태그용 CSRF
 * 토큰, 브랜딩 로고 존재 여부, local 프로파일 플래그.
 */
@ControllerAdvice
public class GlobalModelAttributes {

    private final SettingService settingService;
    private final AppProperties appProperties;
    private final BrandingService brandingService;
    private final R2StorageService storageService;
    private final boolean local;

    public GlobalModelAttributes(SettingService settingService, AppProperties appProperties,
                                 BrandingService brandingService, R2StorageService storageService,
                                 Environment environment) {
        this.settingService = settingService;
        this.appProperties = appProperties;
        this.brandingService = brandingService;
        this.storageService = storageService;
        this.local = environment.matchesProfiles("local");
    }

    @ModelAttribute("appName")
    public String appName() {
        return appProperties.name();
    }

    @ModelAttribute("setting")
    public Setting setting() {
        return settingService.get();
    }

    @ModelAttribute("currentUri")
    public String currentUri(HttpServletRequest request) {
        return request.getRequestURI();
    }

    @ModelAttribute("currentUrl")
    public String currentUrl(HttpServletRequest request) {
        return request.getRequestURL().toString();
    }

    /** 프로필 이미지 아바타용 공개 스토리지 기본 URL (R2 도메인 또는 /storage). */
    @ModelAttribute("r2PublicUrl")
    public String r2PublicUrl() {
        return storageService.publicBaseUrl();
    }

    @ModelAttribute("hasLogoColorSvg")
    public boolean hasLogoColorSvg() {
        return brandingService.exists("logo_color.svg");
    }

    @ModelAttribute("hasLogoWhiteSvg")
    public boolean hasLogoWhiteSvg() {
        return brandingService.exists("logo_white.svg");
    }

    /** 로컬 전용 UI를 제어 (빠른 로그인, 사이드바 "데이터베이스" 항목). */
    @ModelAttribute("isLocal")
    public boolean isLocal() {
        return local;
    }

    /** 일회성 세션 플래시를 토스트 모델 속성으로 옮긴다. */
    @ModelAttribute
    public void flash(HttpServletRequest request, Model model) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return;
        }
        consume(session, model, FlashMessage.MESSAGE, "message");
        consume(session, model, FlashMessage.SUCCESS, "success");
        consume(session, model, FlashMessage.ERROR, "error");
    }

    private void consume(HttpSession session, Model model, String sessionKey, String modelKey) {
        Object value = session.getAttribute(sessionKey);
        if (value != null) {
            model.addAttribute(modelKey, value);
            session.removeAttribute(sessionKey);
        }
    }

    @ModelAttribute("_csrf")
    public CsrfToken csrf(HttpServletRequest request) {
        Object token = request.getAttribute(CsrfToken.class.getName());
        if (token instanceof CsrfToken csrfToken) {
            return csrfToken;
        }
        Object fallback = request.getAttribute("_csrf");
        return fallback instanceof CsrfToken csrfToken ? csrfToken : null;
    }
}
