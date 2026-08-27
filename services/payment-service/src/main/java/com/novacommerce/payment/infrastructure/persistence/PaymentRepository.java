package com.novacommerce.payment.infrastructure.persistence;

import com.novacommerce.payment.domain.Payment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    @org.springframework.data.jpa.repository.Lock(LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select p from Payment p where p.id = :id")
    Optional<Payment> lockById(@org.springframework.data.repository.query.Param("id") UUID id);

    List<Payment> findByOrderId(UUID orderId);

    Optional<Payment> findByGatewayTransactionId(String gatewayTransactionId);

    List<Payment> findByOrderIdAndCustomerId(UUID orderId, UUID customerId);
}
