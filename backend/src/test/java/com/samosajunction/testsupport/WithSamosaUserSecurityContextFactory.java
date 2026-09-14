package com.samosajunction.testsupport;

import com.samosajunction.auth.security.UserPrincipal;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.util.List;
import java.util.UUID;

public class WithSamosaUserSecurityContextFactory implements WithSecurityContextFactory<WithSamosaUser> {

    @Override
    public SecurityContext createSecurityContext(WithSamosaUser annotation) {
        var principal = new UserPrincipal(
                UUID.fromString(annotation.id()),
                annotation.email(),
                "n/a",
                true,
                List.of(new SimpleGrantedAuthority("ROLE_" + annotation.role()))
        );
        var authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.getAuthorities()
        );
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        return context;
    }
}
