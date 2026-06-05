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
 * Manual authentication + session establishment for the signin flow
 * (login / signup / OTP reset / quick login), mirroring the reference's
 * {@code Auth::attempt} / {@code Auth::login}. Persists the security context to
 * the (Spring Session-backed) HTTP session and rotates the session id.
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

    /** Verify credentials (throws {@code AuthenticationException} on failure). */
    public Authentication authenticate(String email, String password) {
        return authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password));
    }

    /** Build an Authentication for a known user (post-signup / reset / quick login). */
    public Authentication tokenFor(User user) {
        SecurityPrincipal principal = SecurityPrincipal.from(user);
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    /**
     * Persist the authentication to the session and return the post-login target
     * (saved "intended" URL, else role default). Reads the saved request before
     * rotating the session id (PRD §6.2).
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
     * Refresh the stored principal in-place (e.g. after a profile-image change)
     * so navbar/sidebar avatars reflect the new state without re-login.
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
