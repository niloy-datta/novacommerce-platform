package com.novacommerce.inventory.domain.reservation;
import jakarta.persistence.*; import java.util.UUID;
@Embeddable public class ReservationItem {
 @Column(name="variant_id",nullable=false) private UUID variantId; @Column(nullable=false) private long quantity;
 protected ReservationItem(){} public ReservationItem(UUID id,long quantity){if(id==null||quantity<=0)throw new IllegalArgumentException("Invalid reservation item");variantId=id;this.quantity=quantity;}
 public UUID getVariantId(){return variantId;} public long getQuantity(){return quantity;}
}
