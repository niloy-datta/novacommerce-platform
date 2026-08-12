package com.novacommerce.auth.application;

import com.novacommerce.auth.api.error.AuthException;
import com.novacommerce.auth.config.AuthProperties;
import com.novacommerce.auth.domain.token.RefreshToken;
import com.novacommerce.auth.domain.user.UserAccount;
import com.novacommerce.auth.infrastructure.persistence.RefreshTokenRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final RefreshTokenRepository tokens;
    private final AuthProperties properties;
    public RefreshTokenService(RefreshTokenRepository tokens, AuthProperties properties) { this.tokens = tokens; this.properties = properties; }

    @Transactional
    public IssuedRefreshToken create(UserAccount user) { return create(user, UUID.randomUUID()); }

    @Transactional(noRollbackFor = AuthException.class)
    public RotatedRefreshToken rotate(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) throw invalid();
        RefreshToken current = tokens.findByTokenHash(hash(rawToken)).orElseThrow(this::invalid);
        Instant now = Instant.now();
        if (current.getRevokedAt() != null) {
            tokens.revokeActiveFamily(current.getFamilyId(), now, "REPLAY_DETECTED");
            throw new AuthException(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_REUSE_DETECTED", "Refresh token reuse detected");
        }
        if (!current.getExpiresAt().isAfter(now)) {
            current.revoke("EXPIRED", now);
            throw invalid();
        }
        current.revoke("ROTATED", now);
        IssuedRefreshToken replacement = create(current.getUser(), current.getFamilyId());
        current.setReplacedByTokenId(replacement.id());
        return new RotatedRefreshToken(current.getUser(), replacement.value());
    }

    @Transactional
    public void revoke(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return;
        tokens.findByTokenHash(hash(rawToken)).ifPresent(token -> {
            if (token.getRevokedAt() == null) token.revoke("LOGOUT", Instant.now());
        });
    }

    private IssuedRefreshToken create(UserAccount user, UUID familyId) {
        String raw = rawToken();
        RefreshToken persisted = tokens.save(new RefreshToken(user, hash(raw), familyId, Instant.now().plus(properties.getJwt().getRefreshTokenTtl())));
        return new IssuedRefreshToken(persisted.getId(), raw);
    }
    private String rawToken() { byte[] bytes = new byte[32]; RANDOM.nextBytes(bytes); return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); }
    private String hash(String raw) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception exception) { throw new IllegalStateException("SHA-256 unavailable", exception); }
    }
    private AuthException invalid() { return new AuthException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "Authentication failed"); }
    public record IssuedRefreshToken(UUID id, String value) { }
    public record RotatedRefreshToken(UserAccount user, String replacementValue) { }
}
