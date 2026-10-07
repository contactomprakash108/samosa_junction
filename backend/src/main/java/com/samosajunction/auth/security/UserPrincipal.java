package com.samosajunction.auth.security;

import com.samosajunction.user.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.UUID;

// AI-ASSISTED: Cursor
// PROMPT: UserDetails principal with separate JWT and credential verification factories
// ACCEPTED-BY: omprakash
public final class UserPrincipal implements UserDetails {

    private final UUID id;
    private final String email;
    private final String passwordHash;
    private final boolean enabled;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(
            UUID id,
            String email,
            String passwordHash,
            boolean enabled,
            Collection<? extends GrantedAuthority> authorities
    ) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.enabled = enabled;
        this.authorities = authorities;
    }

    /** Used by {@code AuthenticationManager} during password login. */
    public static UserPrincipal forCredentialVerification(User user) {
        return new UserPrincipal(
                user.getId(),
                user.getEmail(),
                user.getPasswordHash(),
                user.isEnabled(),
                toAuthorities(user)
        );
    }

    /** Used after JWT validation; password hash is not needed in the security context. */
    public static UserPrincipal forAuthenticatedUser(User user) {
        return new UserPrincipal(
                user.getId(),
                user.getEmail(),
                null,
                user.isEnabled(),
                toAuthorities(user)
        );
    }

    private static Collection<? extends GrantedAuthority> toAuthorities(User user) {
        return user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName().name()))
                .toList();
    }

    public UUID getId() {
        return id;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
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
        return enabled;
    }
}
