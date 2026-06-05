package com.example.bp.mapper;

import java.time.LocalDateTime;

import com.example.bp.domain.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** MyBatis mapper for {@code users}. Extended in later PRs (search/paging/CRUD). */
@Mapper
public interface UserMapper {

    User findByEmail(@Param("email") String email);

    User findById(@Param("id") Long id);

    /** First user with the given role — local quick login (PRD §6.4). */
    User findFirstByRole(@Param("role") String role);

    boolean existsByEmail(@Param("email") String email);

    long count();

    int insert(User user);

    // ── OTP password-reset state (PRD §FR-2.3) ──────────────────────────────
    int updateOtp(@Param("id") Long id, @Param("otp") String otp,
                  @Param("otpExpiresAt") LocalDateTime otpExpiresAt);

    int incrementOtpAttempts(@Param("id") Long id);

    int invalidateOtp(@Param("id") Long id);

    /** Set new password, mark verified, and clear OTP state in one statement. */
    int updatePasswordAndVerify(@Param("id") Long id, @Param("password") String password);
}
