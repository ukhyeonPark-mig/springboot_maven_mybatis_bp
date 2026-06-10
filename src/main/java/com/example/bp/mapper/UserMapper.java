package com.example.bp.mapper;

import java.time.LocalDateTime;

import com.example.bp.domain.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** {@code users}를 위한 MyBatis mapper. 이후 PR에서 확장됨 (검색/페이징/CRUD). */
@Mapper
public interface UserMapper {

    User findByEmail(@Param("email") String email);

    User findById(@Param("id") Long id);

    /** 주어진 역할을 가진 첫 번째 사용자 — 로컬 빠른 로그인 (PRD §6.4). */
    User findFirstByRole(@Param("role") String role);

    boolean existsByEmail(@Param("email") String email);

    long count();

    int insert(User user);

    // ── OTP 비밀번호 재설정 상태 (PRD §FR-2.3) ──────────────────────────────
    int updateOtp(@Param("id") Long id, @Param("otp") String otp,
                  @Param("otpExpiresAt") LocalDateTime otpExpiresAt);

    int incrementOtpAttempts(@Param("id") Long id);

    int invalidateOtp(@Param("id") Long id);

    /** 새 비밀번호 설정, 인증 완료 표시, OTP 상태 초기화를 한 statement로 처리. */
    int updatePasswordAndVerify(@Param("id") Long id, @Param("password") String password);

    int updatePassword(@Param("id") Long id, @Param("password") String password);

    int updateProfileImage(@Param("id") Long id, @Param("profileImage") String profileImage);

    // ── 관리자 사용자 관리 (FR-9) ────────────────────────────────────────
    java.util.List<User> search(@Param("search") String search, @Param("numericId") Integer numericId,
                                @Param("role") String role, @Param("limit") int limit, @Param("offset") int offset);

    long countSearch(@Param("search") String search, @Param("numericId") Integer numericId,
                     @Param("role") String role);

    long countByRole(@Param("role") String role);

    int updateNameRole(@Param("id") Long id, @Param("name") String name, @Param("role") String role);

    int deleteById(@Param("id") Long id);
}
