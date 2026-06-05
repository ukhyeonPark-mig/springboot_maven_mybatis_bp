package com.example.bp.service;

import java.time.LocalDateTime;

import com.example.bp.domain.User;
import com.example.bp.mapper.UserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/** User account operations shared by signin (signup/reset) and admin (PR8). */
@Service
public class UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public User findByEmail(String email) {
        return userMapper.findByEmail(email);
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
}
