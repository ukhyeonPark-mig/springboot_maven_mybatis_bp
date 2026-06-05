package com.example.bp.security;

import java.util.Collection;
import java.util.List;

import com.example.bp.domain.Role;
import com.example.bp.domain.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Authenticated principal. Carries display fields (name/email/role/profileImage)
 * so navbar/sidebar avatars render without re-querying (PRD §15.4).
 */
public class SecurityPrincipal implements UserDetails {

    private final Long id;
    private final String email;
    private final String name;
    private final String role;          // "client" | "admin"
    private final String profileImage;
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;

    public SecurityPrincipal(Long id, String email, String name, String role,
                             String profileImage, String password,
                             Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.role = role;
        this.profileImage = profileImage;
        this.password = password;
        this.authorities = authorities;
    }

    public static SecurityPrincipal from(User user) {
        Role role = Role.fromValue(user.getRole());
        return new SecurityPrincipal(
                user.getId(), user.getEmail(), user.getName(), role.value(),
                user.getProfileImage(), user.getPassword(),
                List.of(new SimpleGrantedAuthority(role.authority())));
    }

    // ── Display helpers (used in templates) ────────────────────────────────
    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public String getRole() {
        return role;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public boolean isAdmin() {
        return Role.ADMIN.value().equalsIgnoreCase(role);
    }

    /** Name when present, otherwise the email (PRD §15.4). */
    public String getDisplayName() {
        return (name != null && !name.isBlank()) ? name : email;
    }

    /** First character for the initial-avatar fallback (PRD §15.4). */
    public String getInitial() {
        String base = getDisplayName();
        return base.isEmpty() ? "" : base.substring(0, 1);
    }

    // ── UserDetails ─────────────────────────────────────────────────────────
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
