package com.proofhold.auth;

public record TokenResponse(String accessToken, String tokenType, int expiresIn, UserResponse user) {

    public static TokenResponse bearer(String accessToken, int expiresIn, UserResponse user) {
        return new TokenResponse(accessToken, "Bearer", expiresIn, user);
    }
}
