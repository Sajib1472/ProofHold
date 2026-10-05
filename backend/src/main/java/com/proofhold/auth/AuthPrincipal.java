package com.proofhold.auth;

import com.proofhold.domain.Role;

public record AuthPrincipal(Long id, String email, Role role) {}
