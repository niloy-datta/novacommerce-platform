package com.novacommerce.inventory.api.dto;
import com.novacommerce.inventory.domain.inventory.*; import com.novacommerce.inventory.domain.reservation.*; import java.time.Instant; import java.util.*;
public final class InventoryDtos {private InventoryDtos(){}
 public record Availability(UUID variantId,String availability,long availableQuantity){}
 public record AdminInventory(UUID variantId,long onHand,long reserved,long available,long version){}
 public record Reservation(UUID id,String status,Instant expiresAt,List<ReservationLine> items){}
 public record ReservationLine(UUID variantId,long quantity){}
 public record Movement(UUID id,UUID variantId,String type,long onHandDelta,long reservedDelta,UUID referenceId,String reason,UUID actorId,Instant createdAt){}
 public static Availability availability(InventoryItem i){return new Availability(i.getVariantId(),i.available()>0?"AVAILABLE":"OUT_OF_STOCK",i.available());}
 public static AdminInventory admin(InventoryItem i){return new AdminInventory(i.getVariantId(),i.getOnHand(),i.getReserved(),i.available(),i.getVersion());}
 public static Reservation reservation(InventoryReservation r){return new Reservation(r.getId(),r.getStatus().name(),r.getExpiresAt(),r.getItems().stream().sorted(Comparator.comparing(ReservationItem::getVariantId)).map(i->new ReservationLine(i.getVariantId(),i.getQuantity())).toList());}
 public static Movement movement(InventoryMovement m){return new Movement(m.getId(),m.getVariantId(),m.getType().name(),m.getOnHandDelta(),m.getReservedDelta(),m.getReferenceId(),m.getReason(),m.getActorId(),m.getCreatedAt());}
}
