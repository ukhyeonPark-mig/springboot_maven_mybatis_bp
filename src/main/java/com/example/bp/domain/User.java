package com.example.bp.domain;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * User account (table {@code users}, PRD §5.1).
 * {@code role} is kept as a raw String for clean MyBatis mapping; use
 * {@link Role#fromValue(String)} where the enum is needed.
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
