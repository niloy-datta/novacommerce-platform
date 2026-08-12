package com.novacommerce.auth.application;

import com.novacommerce.auth.api.dto.LoginRequest;
import com.novacommerce.auth.api.error.AuthException;
import com.novacommerce.auth.domain.user.UserAccount;
import com.novacommerce.auth.domain.user.UserStatus;
import com.novacommerce.auth.infrastructure.persistence.UserAccountRepository;
import com.novacommerce.auth.infrastructure.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticationService {
    private final UserAccountRepository users;
    private final RegistrationService registration;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokens;
    private final JwtService jwtService;
    public AuthenticationService(UserAccountRepository users, RegistrationService registration, PasswordEncoder passwordEncoder,
                                 RefreshTokenService refreshTokens, JwtService jwtService) {
        this.users = users; this.registration = registration; this.passwordEncoder = passwordEncoder;
        this.refreshTokens = refreshTokens; this.jwtService = jwtService;
    }

    @Transactional
    public LoginResult login(LoginRequest request) {
        UserAccount user = users.findByEmailNormalized(registration.normalize(request.email())).orElseThrow(this::invalidCredentials);
        if (user.getStatus() != UserStatus.ACTIVE || !passwordEncoder.matches(request.password(), user.getPasswordHash())) throw invalidCredentials();
        RefreshTokenService.IssuedRefreshToken refresh = refreshTokens.create(user);
        return new LoginResult(user, jwtService.issue(user), refresh.value());
    }
    @Transactional(noRollbackFor = AuthException.class)
    public RefreshResult refresh(String rawToken) {
        RefreshTokenService.RotatedRefreshToken refresh = refreshTokens.rotate(rawToken);
        return new RefreshResult(refresh.user(), jwtService.issue(refresh.user()), refresh.replacementValue());
    }
    private AuthException invalidCredentials() { return new AuthException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Authentication failed"); }
    public record LoginResult(UserAccount user, String accessToken, String refreshToken) { }
    public record RefreshResult(UserAccount user, String accessToken, String refreshToken) { }
}
