package com.novacommerce.notification.infrastructure.persistence;

import com.novacommerce.notification.domain.Notification;
import com.novacommerce.notification.domain.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findByRecipientOrderByCreatedAtDesc(String recipient);

    List<Notification> findByStatus(NotificationStatus status);

    java.util.Optional<Notification> findBySourceEventId(String sourceEventId);

    List<Notification> findTop50ByStatusOrderByCreatedAtAsc(NotificationStatus status);
}
