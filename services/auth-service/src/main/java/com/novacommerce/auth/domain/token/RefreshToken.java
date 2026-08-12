package com.novacommerce.auth.domain.token;

import com.novacommerce.auth.domain.user.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {
    @Id
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;
    @Column(name = "family_id", nullable = false)
    private UUID familyId;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
    @Column(name = "last_used_at")
    private Instant lastUsedAt;
    @Column(name = "revoked_at")
    private Instant revokedAt;
    @Column(name = "revoke_reason", length = 64)
    private String revokeReason;
    @Column(name = "replaced_by_token_id")
    private UUID replacedByTokenId;

    protected RefreshToken() { }
    public RefreshToken(UserAccount user, String tokenHash, UUID familyId, Instant expiresAt) {
        this.id = UUID.randomUUID(); this.user = user; this.tokenHash = tokenHash;
        this.familyId = familyId; this.createdAt = Instant.now(); this.expiresAt = expiresAt;
    }
    public void revoke(String reason, Instant now) { this.revokedAt = now; this.revokeReason = reason; this.lastUsedAt = now; }
    public void setReplacedByTokenId(UUID id) { this.replacedByTokenId = id; }
    public UUID getId() { return id; }
    public String getTokenHash() { return tokenHash; }
    public UserAccount getUser() { return user; }
    public UUID getFamilyId() { return familyId; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getRevokedAt() { return revokedAt; }
}
