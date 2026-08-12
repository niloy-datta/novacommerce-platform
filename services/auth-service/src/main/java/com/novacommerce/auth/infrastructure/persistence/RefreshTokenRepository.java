package com.novacommerce.auth.infrastructure.persistence;

import com.novacommerce.auth.domain.token.RefreshToken;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("update RefreshToken token set token.revokedAt = :now, token.revokeReason = :reason "
        + "where token.familyId = :familyId and token.revokedAt is null")
    int revokeActiveFamily(@Param("familyId") UUID familyId, @Param("now") Instant now, @Param("reason") String reason);
}
