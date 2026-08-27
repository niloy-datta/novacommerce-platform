package com.novacommerce.payment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "processed_webhook_events")
public class ProcessedWebhookEvent {
    @Id @Column(name = "provider_event_id", length = 200) private String providerEventId;
    @Column(name = "event_type", nullable = false, length = 100) private String eventType;
    @Column(name = "received_at", nullable = false) private Instant receivedAt;
    protected ProcessedWebhookEvent() { }
    public ProcessedWebhookEvent(String id, String type, Instant receivedAt) { providerEventId = id; eventType = type; this.receivedAt = receivedAt; }
    public String getProviderEventId() { return providerEventId; }
    public String getEventType() { return eventType; }
    public Instant getReceivedAt() { return receivedAt; }
}
