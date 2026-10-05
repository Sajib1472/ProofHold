package com.proofhold.auth;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

public final class AuthPrincipals {

    private AuthPrincipals() {}

    public static AuthPrincipal require() {
        return optional().orElseThrow(UnauthorizedException::new);
    }

    public static Optional<AuthPrincipal> optional() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthPrincipal principal)) {
            return Optional.empty();
        }
        return Optional.of(principal);
    }
}
