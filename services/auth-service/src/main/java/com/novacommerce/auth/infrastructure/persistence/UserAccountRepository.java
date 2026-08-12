package com.novacommerce.auth.infrastructure.persistence;

import com.novacommerce.auth.domain.user.UserAccount;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {
    Optional<UserAccount> findByEmailNormalized(String emailNormalized);
    boolean existsByEmailNormalized(String emailNormalized);
}
