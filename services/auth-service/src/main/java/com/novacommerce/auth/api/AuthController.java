package com.novacommerce.auth.api;

import com.novacommerce.auth.api.dto.CsrfResponse;
import com.novacommerce.auth.api.dto.LoginRequest;
import com.novacommerce.auth.api.dto.LoginResponse;
import com.novacommerce.auth.api.dto.RegisterRequest;
import com.novacommerce.auth.api.dto.UserResponse;
import com.novacommerce.auth.application.AuthenticationService;
import com.novacommerce.auth.application.RefreshTokenService;
import com.novacommerce.auth.application.RegistrationService;
import com.novacommerce.auth.config.AuthProperties;
import com.novacommerce.auth.domain.user.UserAccount;
import com.novacommerce.auth.domain.user.UserStatus;
import com.novacommerce.auth.infrastructure.persistence.UserAccountRepository;
import com.novacommerce.auth.infrastructure.security.CookieBearerTokenResolver;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;
import com.novacommerce.auth.api.error.AuthException;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private static final String REFRESH_COOKIE = "NC_REFRESH";
    private final RegistrationService registration;
    private final AuthenticationService authentication;
    private final RefreshTokenService refreshTokens;
    private final UserAccountRepository users;
    private final AuthProperties properties;
    private final RSAKey signingJwk;

    public AuthController(RegistrationService registration, AuthenticationService authentication, RefreshTokenService refreshTokens,
                          UserAccountRepository users, AuthProperties properties, RSAKey signingJwk) {
        this.registration = registration; this.authentication = authentication; this.refreshTokens = refreshTokens;
        this.users = users; this.properties = properties; this.signingJwk = signingJwk;
    }

    @GetMapping("/csrf")
    public CsrfResponse csrf(CsrfToken token) { return new CsrfResponse(token.getToken(), token.getHeaderName()); }

    @GetMapping("/jwks")
    public Map<String, Object> jwks() {
        return new JWKSet(signingJwk.toPublicJWK()).toJSONObject();
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(registration.register(request)));
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        AuthenticationService.LoginResult result = authentication.login(request);
        writeAuthCookies(response, result.accessToken(), result.refreshToken());
        return new LoginResponse(UserResponse.from(result.user()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<Void> refresh(HttpServletRequest request, HttpServletResponse response) {
        try {
            AuthenticationService.RefreshResult result = authentication.refresh(cookie(request, REFRESH_COOKIE));
            writeAuthCookies(response, result.accessToken(), result.refreshToken());
            return ResponseEntity.noContent().build();
        } catch (AuthException exception) {
            clearAuthCookies(response);
            throw exception;
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        refreshTokens.revoke(cookie(request, REFRESH_COOKIE));
        clearAuthCookies(response);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        UUID id;
        try { id = UUID.fromString(jwt.getSubject()); } catch (IllegalArgumentException exception) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED", "Authentication required");
        }
        UserAccount user = users.findById(id).filter(account -> account.getStatus() == UserStatus.ACTIVE)
            .orElseThrow(() -> new AuthException(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED", "Authentication required"));
        return UserResponse.from(user);
    }

    private void writeAuthCookies(HttpServletResponse response, String access, String refresh) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie(CookieBearerTokenResolver.ACCESS_COOKIE, access, "/", properties.getJwt().getAccessTokenTtl()).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, cookie(REFRESH_COOKIE, refresh, "/api/v1/auth", properties.getJwt().getRefreshTokenTtl()).toString());
    }
    private void clearAuthCookies(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie(CookieBearerTokenResolver.ACCESS_COOKIE, "", "/", Duration.ZERO).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, cookie(REFRESH_COOKIE, "", "/api/v1/auth", Duration.ZERO).toString());
    }
    private ResponseCookie cookie(String name, String value, String path, Duration duration) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value).httpOnly(true).secure(properties.getCookie().isSecure())
            .sameSite(properties.getCookie().getSameSite()).path(path).maxAge(duration);
        if (properties.getCookie().getDomain() != null && !properties.getCookie().getDomain().isBlank()) builder.domain(properties.getCookie().getDomain());
        return builder.build();
    }
    private String cookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies(); if (cookies == null) return null;
        for (Cookie cookie : cookies) if (name.equals(cookie.getName())) return cookie.getValue();
        return null;
    }
}
