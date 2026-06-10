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
 * 세션 기반 인가 담당 (필터체인에 정의)
*                  → 이 클래스는 "권한 확인"만 하고, 실제 "로그인 검증(인증)"은 직접 안 한다 (인증 자체는 signin 컨트롤러에서 
*                    수동으로 수행한다 (rate-limit + Turnstile + HTMX fragment).)
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
                // 권한에 따른 url 허용 여부 설정
                .requestMatchers("/", "/signin", "/signin/**", "/contact", "/privacy", "/terms", "/sitemap.xml").permitAll()
                .requestMatchers("/css/**", "/js/**", "/image/**", "/branding/**", "/theme/**", "/favicon.ico", "/error").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/client/**").authenticated()
                .anyRequest().authenticated())
            // 진입점 전용: 미인증 사용자를 /signin으로 리다이렉트한다 (의도한 URL 저장).
            .formLogin(form -> form
                .loginPage("/signin")
                .permitAll())
            // 로그아웃 처리: 컨트롤러 매핑, 세션 정리, 쿠키 삭제
            .logout(logout -> logout
                .logoutRequestMatcher(new OrRequestMatcher(
                        new AntPathRequestMatcher("/logout", "GET"),
                        new AntPathRequestMatcher("/admin/logout", "GET")))
                .logoutSuccessUrl("/")
                .invalidateHttpSession(true)
                .deleteCookies("SESSION"));
        return http.build();
    }
}
