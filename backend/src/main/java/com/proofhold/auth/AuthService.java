package com.proofhold.auth;

import com.proofhold.domain.Role;
import com.proofhold.user.User;
import com.proofhold.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final JwtService jwtService;

    public AuthService(UserRepository users, PasswordEncoder passwords, JwtService jwtService) {
        this.users = users;
        this.passwords = passwords;
        this.jwtService = jwtService;
    }

    public TokenResponse login(LoginRequest request) {
        User user = users.findByEmail(request.email().trim().toLowerCase())
                .orElseThrow(UnauthorizedException::new);
        if (!passwords.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException();
        }
        return tokens(user);
    }

    @Transactional
    public TokenResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (users.existsByEmail(email)) {
            throw new DuplicateEmailException(email);
        }
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwords.encode(request.password()));
        user.setRole(Role.CLAIMER);
        users.save(user);
        return tokens(user);
    }

    private TokenResponse tokens(User user) {
        return TokenResponse.bearer(jwtService.issue(user), jwtService.accessSeconds(), UserResponse.from(user));
    }
}
