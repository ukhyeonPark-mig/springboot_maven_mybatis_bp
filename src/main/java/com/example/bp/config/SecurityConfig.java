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
 * Session-based authorization (PRD §6). Authentication itself is performed
 * manually in the signin controller (rate-limit + Turnstile + HTMX fragments,
 * see {@code SigninController} / {@code AuthSessionService}); form-login here
 * only provides the {@code /signin} entry point that preserves the intended URL.
 * <ul>
 *   <li>permitAll: public pages + static assets</li>
 *   <li>/client/** authenticated, /admin/** ROLE_ADMIN</li>
 *   <li>unauthenticated protected access -> /signin (intended URL saved)</li>
 *   <li>authenticated non-admin -> 403 (Boot resolves templates/error/403.html)</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** BCrypt cost 12 (PRD §6.1, reference BCRYPT_ROUNDS=12). */
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
                .requestMatchers("/css/**", "/js/**", "/branding/**", "/theme/**", "/favicon.ico", "/error").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/client/**").authenticated()
                .anyRequest().authenticated())
            // Entry point only: redirect unauthenticated users to /signin (saving the intended URL).
            .formLogin(form -> form
                .loginPage("/signin")
                .permitAll())
            .logout(logout -> logout
                // Navbar/sidebar use GET links (/logout, /admin/logout) — match both
                .logoutRequestMatcher(new OrRequestMatcher(
                        new AntPathRequestMatcher("/logout", "GET"),
                        new AntPathRequestMatcher("/admin/logout", "GET")))
                .logoutSuccessUrl("/")
                .invalidateHttpSession(true)
                .deleteCookies("SESSION"));
        return http.build();
    }
}
