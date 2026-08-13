package com.novacommerce.inventory.infrastructure.persistence;
import com.novacommerce.inventory.domain.reservation.InventoryReservation; import jakarta.persistence.LockModeType; import java.time.Instant; import java.util.*; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param;
public interface InventoryReservationRepository extends JpaRepository<InventoryReservation,UUID>{
 Optional<InventoryReservation> findByIdempotencyKey(String key);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select r from InventoryReservation r where r.id=:id") Optional<InventoryReservation> lockById(@Param("id") UUID id);
 @Query(value="select id from inventory_reservations where status='ACTIVE' and expires_at <= :now order by expires_at limit :batch for update skip locked",nativeQuery=true) List<UUID> claimExpired(@Param("now") Instant now,@Param("batch") int batch);
}
