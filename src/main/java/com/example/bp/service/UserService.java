package com.example.bp.service;

import java.time.LocalDateTime;
import java.util.List;

import com.example.bp.domain.User;
import com.example.bp.mapper.UserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/** signin(회원가입/재설정)과 admin(PR8)이 공유하는 사용자 계정 작업. */
@Service
public class UserService {

    public static final int PAGE_SIZE = 10;

    /** 관리자 목록용 사용자 한 페이지와 페이지네이션/역할 카운트 메타데이터. */
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

    /** 계정을 생성한다. 원시 비밀번호를 BCrypt로 해싱한다 (PRD §6.1). */
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

    // ── OTP 상태 ───────────────────────────────────────────────────────────
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

    // ── 프로필 / 비밀번호 (client + admin 계정) ─────────────────────────
    public boolean checkPassword(String rawPassword, String hashed) {
        return passwordEncoder.matches(rawPassword, hashed);
    }

    public void changePassword(Long userId, String rawNewPassword) {
        userMapper.updatePassword(userId, passwordEncoder.encode(rawNewPassword));
    }

    public void updateProfileImage(Long userId, String filename) {
        userMapper.updateProfileImage(userId, filename);
    }

    // ── 관리자 사용자 관리 (FR-9) ────────────────────────────────────────
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
