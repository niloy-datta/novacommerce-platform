package com.novacommerce.notification.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    private UUID id;

    @Column(name = "recipient", nullable = false)
    private String recipient;

    @Column(name = "subject", nullable = false)
    private String subject;

    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(name = "source_event_id", nullable = false, unique = true, length = 200)
    private String sourceEventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private NotificationStatus status;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    protected Notification() {
    }

    public Notification(UUID id, String recipient, String subject, String body, String eventType,
                        NotificationStatus status, int retryCount, Instant createdAt, Instant sentAt) {
        this(id, recipient, subject, body, eventType, UUID.randomUUID().toString(), status, retryCount, createdAt, sentAt);
    }

    public Notification(UUID id, String recipient, String subject, String body, String eventType, String sourceEventId,
                        NotificationStatus status, int retryCount, Instant createdAt, Instant sentAt) {
        this.id = id;
        this.recipient = recipient;
        this.subject = subject;
        this.body = body;
        this.eventType = eventType;
        this.sourceEventId = sourceEventId;
        this.status = status;
        this.retryCount = retryCount;
        this.createdAt = createdAt;
        this.sentAt = sentAt;
    }

    public static Notification create(String recipient, String subject, String body, String eventType) {
        return create(UUID.randomUUID().toString(), recipient, subject, body, eventType);
    }

    public static Notification create(String sourceEventId, String recipient, String subject, String body, String eventType) {
        return new Notification(UUID.randomUUID(), recipient, subject, body, eventType, sourceEventId, NotificationStatus.PENDING, 0, Instant.now(), null);
    }

    public void markSent() {
        this.status = NotificationStatus.SENT;
        this.sentAt = Instant.now();
    }

    public void markFailed() {
        this.retryCount++;
        if (this.retryCount >= 3) {
            this.status = NotificationStatus.FAILED;
        }
    }

    public UUID getId() {
        return id;
    }

    public String getRecipient() {
        return recipient;
    }

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
    }

    public String getEventType() {
        return eventType;
    }

    public String getSourceEventId() { return sourceEventId; }

    public NotificationStatus getStatus() {
        return status;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getSentAt() {
        return sentAt;
    }
}
