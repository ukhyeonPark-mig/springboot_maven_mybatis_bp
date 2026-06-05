package com.example.bp.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Password policy regex per PRD §6.3 (8–15 chars, lower+upper+digit+special !@#$%^&*). */
class PasswordPolicyTest {

    @Test
    void acceptsCompliantPasswords() {
        assertThat(PasswordPolicy.isValid("Abcdef1!")).isTrue();       // 8 chars, all classes
        assertThat(PasswordPolicy.isValid("Password1@")).isTrue();
        assertThat(PasswordPolicy.isValid("Aa1!aaaaaaaaaa")).isTrue(); // 14 chars
    }

    @Test
    void rejectsNonCompliantPasswords() {
        assertThat(PasswordPolicy.isValid(null)).isFalse();
        assertThat(PasswordPolicy.isValid("Abc1!")).isFalse();          // too short (5)
        assertThat(PasswordPolicy.isValid("Abcdefg1!2345678")).isFalse(); // too long (16)
        assertThat(PasswordPolicy.isValid("abcdef1!")).isFalse();        // no uppercase
        assertThat(PasswordPolicy.isValid("ABCDEF1!")).isFalse();        // no lowercase
        assertThat(PasswordPolicy.isValid("Abcdefg!")).isFalse();        // no digit
        assertThat(PasswordPolicy.isValid("Abcdefg1")).isFalse();        // no special
        assertThat(PasswordPolicy.isValid("Abcdef1 ")).isFalse();        // disallowed char (space)
    }
}
