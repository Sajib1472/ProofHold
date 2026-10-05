package com.proofhold.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Role is never accepted from the client. v1 always creates CLAIMER. */
public record RegisterRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8) String password) {}
