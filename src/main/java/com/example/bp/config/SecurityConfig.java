package com.example.bp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;

/**
 * 세션 기반 인가 (PRD §6). 인증 자체는 signin 컨트롤러에서 수동으로 수행한다
 * (rate-limit + Turnstile + HTMX fragment, {@code SigninController} /
 * {@code AuthSessionService} 참고). 여기서의 form-login은 의도한 URL을 보존하는
 * {@code /signin} 진입점만 제공한다.
 * <ul>
 *   <li>permitAll: 공개 페이지 + 정적 자산</li>
 *   <li>/client/** 인증 필요, /admin/** ROLE_ADMIN</li>
 *   <li>미인증 상태의 보호 리소스 접근 -> /signin (의도한 URL 저장)</li>
 *   <li>인증되었으나 admin이 아닌 경우 -> 403 (Boot가 templates/error/403.html을 해석)</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** BCrypt cost 12 (PRD §6.1, 참조 BCRYPT_ROUNDS=12). */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/signin", "/signin/**", "/contact", "/privacy", "/terms", "/sitemap.xml").permitAll()
                .requestMatchers("/css/**", "/js/**", "/image/**", "/branding/**", "/theme/**", "/favicon.ico", "/error").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/client/**").authenticated()
                .anyRequest().authenticated())
            // 진입점 전용: 미인증 사용자를 /signin으로 리다이렉트한다 (의도한 URL 저장).
            .formLogin(form -> form
                .loginPage("/signin")
                .permitAll())
            .logout(logout -> logout
                // navbar/sidebar는 GET 링크(/logout, /admin/logout)를 사용 — 둘 다 매칭
                .logoutRequestMatcher(new OrRequestMatcher(
                        new AntPathRequestMatcher("/logout", "GET"),
                        new AntPathRequestMatcher("/admin/logout", "GET")))
                .logoutSuccessUrl("/")
                .invalidateHttpSession(true)
                .deleteCookies("SESSION"));
        return http.build();
    }
}
