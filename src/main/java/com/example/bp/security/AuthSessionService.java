package com.example.bp.security;

import com.example.bp.domain.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;

/**
 * signin 플로우(login / signup / OTP reset / quick login)를 위한 수동 인증 +
 * 세션 확립으로, 레퍼런스의 {@code Auth::attempt} / {@code Auth::login}을 모방한다.
 * 보안 컨텍스트를 (Spring Session 기반) HTTP 세션에 보존하고 session id를 회전시킨다.
 */
@Component
public class AuthSessionService {

    private static final String ADMIN_AUTHORITY = "ROLE_ADMIN";

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();
    private final RequestCache requestCache = new HttpSessionRequestCache();

    public AuthSessionService(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    /** 자격 증명을 검증한다 (실패 시 {@code AuthenticationException}을 던진다). */
    public Authentication authenticate(String email, String password) {
        return authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password));
    }

    /** 알려진 사용자에 대한 Authentication을 생성한다 (signup 후 / reset / quick login). */
    public Authentication tokenFor(User user) {
        SecurityPrincipal principal = SecurityPrincipal.from(user);
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    /**
     * 인증을 세션에 보존하고 로그인 후 이동 대상을 반환한다
     * (저장된 "intended" URL, 없으면 역할 기본값). session id를 회전시키기 전에
     * 저장된 요청을 읽는다 (PRD §6.2).
     */
    public String establishAndResolveTarget(Authentication authentication,
                                            HttpServletRequest request, HttpServletResponse response) {
        String target = resolveTarget(request, response, authentication);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
        request.changeSessionId();

        return target;
    }

    /**
     * 저장된 principal을 제자리에서 갱신한다 (예: 프로필 이미지 변경 후)
     * 재로그인 없이 navbar/sidebar 아바타가 새 상태를 반영하도록.
     */
    public void refresh(User user, HttpServletRequest request, HttpServletResponse response) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(tokenFor(user));
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }

    private String resolveTarget(HttpServletRequest request, HttpServletResponse response,
                                 Authentication authentication) {
        SavedRequest saved = requestCache.getRequest(request, response);
        if (saved != null) {
            return saved.getRedirectUrl();
        }
        boolean admin = authentication.getAuthorities().stream()
                .anyMatch(a -> ADMIN_AUTHORITY.equals(a.getAuthority()));
        String context = request.getContextPath();
        return admin ? context + "/admin/dashboard" : context + "/";
    }
}
