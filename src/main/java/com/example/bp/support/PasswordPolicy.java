package com.example.bp.support;

import java.util.regex.Pattern;

/**
 * 공유 비밀번호 정책 (PRD §6.3): 8~15자이며 소문자, 대문자, 숫자,
 * {@code !@#$%^&*}의 특수문자를 각각 최소 1자 포함. 레퍼런스와 동일한 regex와
 * 한국어 메시지를 사용한다.
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
