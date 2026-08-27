package com.novacommerce.payment.infrastructure.persistence;

import com.novacommerce.payment.domain.ProcessedWebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedWebhookEventRepository extends JpaRepository<ProcessedWebhookEvent, String> { }
