package com.proofhold.auth;

import com.proofhold.domain.Role;
import com.proofhold.user.User;

public record UserResponse(Long id, String email, Role role) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getRole());
    }

    public static UserResponse from(AuthPrincipal principal) {
        return new UserResponse(principal.id(), principal.email(), principal.role());
    }
}
