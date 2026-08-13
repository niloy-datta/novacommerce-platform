package com.novacommerce.inventory.api.dto;
import jakarta.validation.Valid; import jakarta.validation.constraints.*; import java.util.*;
public final class InventoryRequests {private InventoryRequests(){}
 public record ReservationRequest(@NotEmpty @Size(max=100) List<@Valid ReservationLine> items){}
 public record ReservationLine(@NotNull UUID variantId,@Positive long quantity){}
 public record BulkAvailabilityRequest(@NotEmpty @Size(max=100) Set<@NotNull UUID> variantIds){}
 public record AdjustmentRequest(@NotNull Long quantityDelta,@NotBlank @Size(max=500) String reason){}
}
