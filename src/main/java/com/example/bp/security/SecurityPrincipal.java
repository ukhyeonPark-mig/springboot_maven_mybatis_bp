package com.example.bp.security;

import java.util.Collection;
import java.util.List;

import com.example.bp.domain.Role;
import com.example.bp.domain.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * 인증된 principal. 표시용 필드(name/email/role/profileImage)를 담고 있어
 * navbar/sidebar 아바타를 재조회 없이 렌더링할 수 있다 (PRD §15.4).
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

    // ── 표시용 헬퍼 (템플릿에서 사용) ────────────────────────────────
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

    /** name이 있으면 name, 없으면 email (PRD §15.4). */
    public String getDisplayName() {
        return (name != null && !name.isBlank()) ? name : email;
    }

    /** 이니셜 아바타 폴백을 위한 첫 글자 (PRD §15.4). */
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
