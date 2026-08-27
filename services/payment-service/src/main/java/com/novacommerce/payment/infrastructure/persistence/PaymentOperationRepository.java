package com.novacommerce.payment.infrastructure.persistence;

import com.novacommerce.payment.domain.PaymentOperation;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface PaymentOperationRepository extends JpaRepository<PaymentOperation, UUID> {
    Optional<PaymentOperation> findByIdempotencyKey(String idempotencyKey);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from PaymentOperation o where o.idempotencyKey = :key")
    Optional<PaymentOperation> lockByIdempotencyKey(@Param("key") String key);
    Optional<PaymentOperation> findByPaymentIdAndType(UUID paymentId, com.novacommerce.payment.domain.PaymentOperationType type);
}
