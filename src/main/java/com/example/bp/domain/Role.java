package com.example.bp.domain;

/**
 * 사용자 역할. {@code users.role}에 소문자 {@link #value}로 저장되며
 * (Laravel 레퍼런스와 일치), Spring 권한 {@code ROLE_*}로 매핑된다.
 */
public enum Role {
    CLIENT("client"),
    ADMIN("admin");

    private final String value;

    Role(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    /** Spring Security 권한, 예: {@code ROLE_ADMIN}. */
    public String authority() {
        return "ROLE_" + name();
    }

    public static Role fromValue(String value) {
        for (Role r : values()) {
            if (r.value.equalsIgnoreCase(value)) {
                return r;
            }
        }
        return CLIENT;
    }
}
