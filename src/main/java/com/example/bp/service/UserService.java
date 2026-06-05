package com.example.bp.service;

import java.time.LocalDateTime;
import java.util.List;

import com.example.bp.domain.User;
import com.example.bp.mapper.UserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/** User account operations shared by signin (signup/reset) and admin (PR8). */
@Service
public class UserService {

    public static final int PAGE_SIZE = 10;

    /** One page of users plus pagination/role-count metadata for the admin list. */
    public record UserPage(List<User> content, int page, int totalPages, long total,
                           long countAll, long countClient, long countAdmin) {
    }

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public User findByEmail(String email) {
        return userMapper.findByEmail(email);
    }

    public User findById(Long id) {
        return userMapper.findById(id);
    }

    public boolean existsByEmail(String email) {
        return userMapper.existsByEmail(email);
    }

    public User findFirstByRole(String role) {
        return userMapper.findFirstByRole(role);
    }

    /** Create an account; BCrypt-hashes the raw password (PRD §6.1). */
    public User create(String email, String name, String rawPassword, String role, boolean verified) {
        User user = new User();
        user.setEmail(email);
        user.setName(name);
        user.setRole(role);
        user.setPassword(passwordEncoder.encode(rawPassword));
        if (verified) {
            user.setEmailVerifiedAt(LocalDateTime.now());
        }
        user.setOtpAttempts(0);
        userMapper.insert(user);
        return user;
    }

    // ── OTP state ───────────────────────────────────────────────────────────
    public void setOtp(Long userId, String otp, LocalDateTime expiresAt) {
        userMapper.updateOtp(userId, otp, expiresAt);
    }

    public void incrementOtpAttempts(Long userId) {
        userMapper.incrementOtpAttempts(userId);
    }

    public void invalidateOtp(Long userId) {
        userMapper.invalidateOtp(userId);
    }

    public void resetPassword(Long userId, String rawPassword) {
        userMapper.updatePasswordAndVerify(userId, passwordEncoder.encode(rawPassword));
    }

    // ── Profile / password (client + admin account) ─────────────────────────
    public boolean checkPassword(String rawPassword, String hashed) {
        return passwordEncoder.matches(rawPassword, hashed);
    }

    public void changePassword(Long userId, String rawNewPassword) {
        userMapper.updatePassword(userId, passwordEncoder.encode(rawNewPassword));
    }

    public void updateProfileImage(Long userId, String filename) {
        userMapper.updateProfileImage(userId, filename);
    }

    // ── Admin user management (FR-9) ────────────────────────────────────────
    public UserPage searchPage(String search, String role, int page) {
        int safePage = Math.max(page, 1);
        Integer numericId = parseNumericId(search);
        long total = userMapper.countSearch(search, numericId, role);
        int totalPages = (int) Math.max(1, Math.ceil((double) total / PAGE_SIZE));
        if (safePage > totalPages) {
            safePage = totalPages;
        }
        int offset = (safePage - 1) * PAGE_SIZE;
        List<User> content = userMapper.search(search, numericId, role, PAGE_SIZE, offset);
        return new UserPage(content, safePage, totalPages, total,
                userMapper.count(), userMapper.countByRole("client"), userMapper.countByRole("admin"));
    }

    public void adminUpdate(Long id, String name, String role, String optionalNewPassword) {
        userMapper.updateNameRole(id, name, role);
        if (optionalNewPassword != null && !optionalNewPassword.isEmpty()) {
            userMapper.updatePassword(id, passwordEncoder.encode(optionalNewPassword));
        }
    }

    public void delete(Long id) {
        userMapper.deleteById(id);
    }

    private static Integer parseNumericId(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(search.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
