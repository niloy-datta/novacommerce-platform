package com.novacommerce.inventory.domain.inventory;
import jakarta.persistence.*; import java.time.Instant; import java.util.UUID;
@Entity @Table(name="inventory_movements")
public class InventoryMovement {
 @Id private UUID id; @Column(name="variant_id",nullable=false) private UUID variantId; @Enumerated(EnumType.STRING) @Column(nullable=false) private MovementType type;
 @Column(name="on_hand_delta",nullable=false) private long onHandDelta; @Column(name="reserved_delta",nullable=false) private long reservedDelta;
 @Column(name="reference_id") private UUID referenceId; @Column(columnDefinition="text") private String reason; @Column(name="actor_id") private UUID actorId; @Column(name="created_at",nullable=false) private Instant createdAt;
 protected InventoryMovement() { }
 public InventoryMovement(UUID variantId, MovementType type, long onHandDelta, long reservedDelta, UUID referenceId, String reason, UUID actorId, Instant now) { id=UUID.randomUUID(); this.variantId=variantId; this.type=type; this.onHandDelta=onHandDelta; this.reservedDelta=reservedDelta; this.referenceId=referenceId; this.reason=reason; this.actorId=actorId; createdAt=now; }
 public UUID getId(){return id;} public UUID getVariantId(){return variantId;} public MovementType getType(){return type;} public long getOnHandDelta(){return onHandDelta;} public long getReservedDelta(){return reservedDelta;} public UUID getReferenceId(){return referenceId;} public String getReason(){return reason;} public UUID getActorId(){return actorId;} public Instant getCreatedAt(){return createdAt;}
}
