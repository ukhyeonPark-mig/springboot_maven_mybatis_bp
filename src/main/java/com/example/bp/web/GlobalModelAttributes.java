package com.example.bp.web;

import com.example.bp.domain.Setting;
import com.example.bp.service.BrandingService;
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
 * Common model attributes available to every view (layouts, navbar, sidebar,
 * footer): app name, settings singleton, current URI for active-nav, CSRF token
 * for the HTMX meta tags, branding-logo presence, and the local-profile flag.
 */
@ControllerAdvice
public class GlobalModelAttributes {

    private final SettingService settingService;
    private final AppProperties appProperties;
    private final BrandingService brandingService;
    private final boolean local;

    public GlobalModelAttributes(SettingService settingService, AppProperties appProperties,
                                 BrandingService brandingService, Environment environment) {
        this.settingService = settingService;
        this.appProperties = appProperties;
        this.brandingService = brandingService;
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

    /** Public R2 base URL for profile-image avatars (PRD §4.1/§15.4). */
    @ModelAttribute("r2PublicUrl")
    public String r2PublicUrl() {
        return appProperties.r2() != null ? appProperties.r2().publicUrl() : null;
    }

    @ModelAttribute("hasLogoColorSvg")
    public boolean hasLogoColorSvg() {
        return brandingService.exists("logo_color.svg");
    }

    @ModelAttribute("hasLogoWhiteSvg")
    public boolean hasLogoWhiteSvg() {
        return brandingService.exists("logo_white.svg");
    }

    /** Drives local-only UI (quick login, the sidebar "데이터베이스" item). */
    @ModelAttribute("isLocal")
    public boolean isLocal() {
        return local;
    }

    /** Move any one-shot session flash into the toast model attributes. */
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
