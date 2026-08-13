package com.novacommerce.inventory.domain.reservation;
import jakarta.persistence.*; import java.time.Instant; import java.util.*;
@Entity @Table(name="inventory_reservations")
public class InventoryReservation {
 @Id private UUID id; @Column(name="idempotency_key",nullable=false,unique=true,length=200) private String idempotencyKey; @Column(name="request_hash",nullable=false,length=64) private String requestHash; @Column(name="owner_id") private UUID ownerId;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private ReservationStatus status; @Column(name="expires_at",nullable=false) private Instant expiresAt; @Column(name="created_at",nullable=false) private Instant createdAt; @Column(name="updated_at",nullable=false) private Instant updatedAt; @Version private long version;
 @ElementCollection(fetch=FetchType.EAGER) @CollectionTable(name="inventory_reservation_items",joinColumns=@JoinColumn(name="reservation_id")) private Set<ReservationItem> items=new LinkedHashSet<>();
 protected InventoryReservation(){}
 public InventoryReservation(UUID id,String key,String hash,UUID ownerId,Instant expiresAt,Instant now,Collection<ReservationItem> items){this.id=id;this.idempotencyKey=key;this.requestHash=hash;this.ownerId=ownerId;this.expiresAt=expiresAt;this.createdAt=this.updatedAt=now;this.status=ReservationStatus.ACTIVE;this.items.addAll(items);}
 public boolean release(Instant now){if(status==ReservationStatus.RELEASED)return false;if(status!=ReservationStatus.ACTIVE)throw new IllegalStateException("Reservation cannot be released from "+status);status=ReservationStatus.RELEASED;updatedAt=now;return true;}
 public boolean commit(Instant now){if(status==ReservationStatus.COMMITTED)return false;if(status!=ReservationStatus.ACTIVE)throw new IllegalStateException("Reservation cannot be committed from "+status);status=ReservationStatus.COMMITTED;updatedAt=now;return true;}
 public boolean expire(Instant now){if(status!=ReservationStatus.ACTIVE||expiresAt.isAfter(now))return false;status=ReservationStatus.EXPIRED;updatedAt=now;return true;}
 public UUID getId(){return id;} public String getIdempotencyKey(){return idempotencyKey;} public String getRequestHash(){return requestHash;} public UUID getOwnerId(){return ownerId;} public ReservationStatus getStatus(){return status;} public Instant getExpiresAt(){return expiresAt;} public Instant getCreatedAt(){return createdAt;} public Set<ReservationItem> getItems(){return Set.copyOf(items);}
}
