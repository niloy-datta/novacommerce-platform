package com.novacommerce.payment.infrastructure.persistence;

import com.novacommerce.payment.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    List<Payment> findByOrderId(UUID orderId);

    Optional<Payment> findByGatewayTransactionId(String gatewayTransactionId);
}
