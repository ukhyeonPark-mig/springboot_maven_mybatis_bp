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
 * 로그인 처리를 직접 담당하는 서비스. signin 화면의 모든 로그인 경로
 * (로그인 / 회원가입 후 자동 로그인 / OTP 비밀번호 재설정 후 / 로컬 퀵 로그인)에서 쓴다.
 *
 * 하는 일:
 *   - 이메일·비밀번호 검증
 *   - 로그인 상태를 HTTP 세션(Spring Session)에 저장 → 이후 요청은 세션으로 신원 확인
 *   - 로그인 직후 이동할 페이지 결정
 *   - 세션 고정 공격 방지를 위해 로그인 시 세션 ID 교체
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

    /** 이메일·비밀번호가 맞는지 검증한다. 틀리면 {@code AuthenticationException}이 발생한다. */
    public Authentication authenticate(String email, String password) {
        return authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password));
    }

    /**
     * 비밀번호 확인 없이, 이미 신원이 확실한 사용자를 "로그인 상태 객체"로 만든다.
     * (회원가입 직후·비밀번호 재설정 후·퀵 로그인처럼 검증이 이미 끝난 경우에 사용)
     */
    public Authentication tokenFor(User user) {
        SecurityPrincipal principal = SecurityPrincipal.from(user);
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    /**
     * 로그인 상태를 세션에 저장하고, 로그인 후 이동할 URL을 돌려준다.
     *
     * 이동 대상: 로그인하려다 막혔던 "원래 가려던 URL"이 있으면 그곳,
     * 없으면 역할별 기본 페이지(admin → /admin/dashboard, 그 외 → /).
     * 세션 ID를 교체하면 저장된 "원래 URL"이 사라지므로, 교체 전에 먼저 읽는다 (PRD §6.2).
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
     * 세션에 저장된 로그인 사용자 정보를 최신 값으로 교체한다. 재로그인 없이 바뀐 정보를
     * 즉시 반영할 때 쓴다 (예: 프로필 이미지 변경/삭제 후 navbar·sidebar 아바타 갱신,
     * 관리자 impersonate로 다른 사용자 전환).
     */
    public void refresh(User user, HttpServletRequest request, HttpServletResponse response) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(tokenFor(user));
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }

    // 로그인 직후 어느 URL로 보낼지 결정하는 헬퍼 메소드
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
