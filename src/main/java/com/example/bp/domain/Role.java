package com.example.bp.domain;

/**
 * User role. Stored in {@code users.role} as the lowercase {@link #value}
 * (matches the Laravel reference); mapped to Spring authority {@code ROLE_*}.
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

    /** Spring Security authority, e.g. {@code ROLE_ADMIN}. */
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
