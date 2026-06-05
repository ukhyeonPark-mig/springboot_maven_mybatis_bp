package com.example.bp.support;

import java.util.regex.Pattern;

/**
 * Shared password policy (PRD §6.3): 8–15 chars with at least one lowercase,
 * uppercase, digit, and special character from {@code !@#$%^&*}. Same regex and
 * Korean message as the reference.
 */
public final class PasswordPolicy {

    public static final String REGEX =
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*])[A-Za-z\\d!@#$%^&*]{8,15}$";

    public static final String MESSAGE =
            "비밀번호는 8~15자이며 대문자, 소문자, 숫자, 특수문자(!@#$%^&*)를 각각 1자 이상 포함해야 합니다.";

    private static final Pattern PATTERN = Pattern.compile(REGEX);

    private PasswordPolicy() {
    }

    public static boolean isValid(String password) {
        return password != null && PATTERN.matcher(password).matches();
    }
}
