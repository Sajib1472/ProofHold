package com.proofhold.auth;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.proofhold.domain.Role;
import com.proofhold.user.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private final byte[] secret;
    private final String issuer;
    private final int accessSeconds;

    public JwtService(
            @Value("${proofhold.jwt.secret}") String secret,
            @Value("${proofhold.jwt.issuer}") String issuer,
            @Value("${proofhold.jwt.access-seconds}") int accessSeconds) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("proofhold.jwt.secret must be at least 32 bytes");
        }
        this.secret = bytes;
        this.issuer = issuer;
        this.accessSeconds = accessSeconds;
    }

    public int accessSeconds() {
        return accessSeconds;
    }

    public String issue(User user) {
        return issue(user.getId(), user.getEmail(), user.getRole());
    }

    public String issue(Long userId, String email, Role role) {
        try {
            Instant now = Instant.now();
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .issuer(issuer)
                    .subject(userId.toString())
                    .claim("email", email)
                    .claim("role", role.name())
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(now.plusSeconds(accessSeconds)))
                    .jwtID(UUID.randomUUID().toString())
                    .build();
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            jwt.sign(new MACSigner(secret));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException("Unable to sign JWT", e);
        }
    }

    public AuthPrincipal parse(String token) {
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            if (!jwt.verify(new MACVerifier(secret))) {
                throw new IllegalArgumentException("Invalid token signature");
            }
            JWTClaimsSet claims = jwt.getJWTClaimsSet();
            if (claims.getExpirationTime().toInstant().isBefore(Instant.now())) {
                throw new IllegalArgumentException("Token expired");
            }
            Role role = Role.valueOf(claims.getStringClaim("role"));
            return new AuthPrincipal(Long.parseLong(claims.getSubject()), claims.getStringClaim("email"), role);
        } catch (ParseException | JOSEException | IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid token", e);
        }
    }
}
