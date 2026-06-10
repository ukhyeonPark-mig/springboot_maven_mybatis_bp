package com.example.bp.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** PRD §6.3에 따른 비밀번호 정책 정규식 (8–15자, 소문자+대문자+숫자+특수문자 !@#$%^&*). */
class PasswordPolicyTest {

    @Test
    void acceptsCompliantPasswords() {
        assertThat(PasswordPolicy.isValid("Abcdef1!")).isTrue();       // 8자, 모든 문자 종류
        assertThat(PasswordPolicy.isValid("Password1@")).isTrue();
        assertThat(PasswordPolicy.isValid("Aa1!aaaaaaaaaa")).isTrue(); // 14자
    }

    @Test
    void rejectsNonCompliantPasswords() {
        assertThat(PasswordPolicy.isValid(null)).isFalse();
        assertThat(PasswordPolicy.isValid("Abc1!")).isFalse();          // 너무 짧음 (5)
        assertThat(PasswordPolicy.isValid("Abcdefg1!2345678")).isFalse(); // 너무 김 (16)
        assertThat(PasswordPolicy.isValid("abcdef1!")).isFalse();        // 대문자 없음
        assertThat(PasswordPolicy.isValid("ABCDEF1!")).isFalse();        // 소문자 없음
        assertThat(PasswordPolicy.isValid("Abcdefg!")).isFalse();        // 숫자 없음
        assertThat(PasswordPolicy.isValid("Abcdefg1")).isFalse();        // 특수문자 없음
        assertThat(PasswordPolicy.isValid("Abcdef1 ")).isFalse();        // 허용되지 않는 문자 (공백)
    }
}
