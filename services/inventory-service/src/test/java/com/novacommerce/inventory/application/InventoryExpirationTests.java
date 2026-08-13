package com.novacommerce.inventory.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.novacommerce.inventory.config.InventoryProperties;
import com.novacommerce.inventory.domain.inventory.InventoryItem;
import com.novacommerce.inventory.domain.reservation.*;
import com.novacommerce.inventory.infrastructure.persistence.*;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class InventoryExpirationTests {
    @Test
    void expiredReservationReleasesOnceUsingControllableClock() {
        Instant now = Instant.parse("2026-01-01T00:20:00Z");
        Clock clock = Clock.fixed(now, ZoneOffset.UTC);
        UUID variantId = UUID.randomUUID();
        UUID reservationId = UUID.randomUUID();
        InventoryItem item = new InventoryItem(variantId, 10, now.minusSeconds(1200));
        item.reserve(3, now.minusSeconds(1200));
        InventoryReservation reservation = new InventoryReservation(reservationId, "expiry-key", "hash", UUID.randomUUID(), now.minusSeconds(300), now.minusSeconds(1200), List.of(new ReservationItem(variantId, 3)));

        InventoryItemRepository items = mock(InventoryItemRepository.class);
        InventoryReservationRepository reservations = mock(InventoryReservationRepository.class);
        InventoryMovementRepository movements = mock(InventoryMovementRepository.class);
        when(reservations.claimExpired(now, 100)).thenReturn(List.of(reservationId), List.of());
        when(reservations.lockById(reservationId)).thenReturn(Optional.of(reservation));
        when(items.lockAllOrdered(any())).thenReturn(List.of(item));
        InventoryProperties properties = new InventoryProperties();
        properties.setExpirationBatchSize(100);
        InventoryService service = new InventoryService(items, reservations, movements, mock(IdempotencyKeyLock.class), new ReservationRequestHasher(), properties, clock, new SimpleMeterRegistry());

        assertThat(service.expireBatch()).isEqualTo(1);
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.EXPIRED);
        assertThat(item.getOnHand()).isEqualTo(10);
        assertThat(item.getReserved()).isZero();
        assertThat(service.expireBatch()).isZero();
        assertThat(item.getReserved()).isZero();
        verify(movements, times(1)).save(any());
    }
}
