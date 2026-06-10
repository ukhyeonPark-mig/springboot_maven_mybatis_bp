package com.example.bp.domain;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 사용자 계정 (테이블 {@code users}, PRD §5.1).
 * {@code role}은 깔끔한 MyBatis 매핑을 위해 raw String으로 유지하며, enum이
 * 필요한 곳에서는 {@link Role#fromValue(String)}을 사용한다.
 */
@Data
public class User {
    private Long id;
    private String role;
    private String email;
    private LocalDateTime emailVerifiedAt;
    private String name;
    private String password;
    private String otp;
    private LocalDateTime otpExpiresAt;
    private int otpAttempts;
    private String profileImage;
    private String rememberToken;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public boolean isAdmin() {
        return Role.ADMIN.value().equalsIgnoreCase(role);
    }
}
