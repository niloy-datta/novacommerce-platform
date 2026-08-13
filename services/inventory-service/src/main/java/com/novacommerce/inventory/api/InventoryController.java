package com.novacommerce.inventory.api;
import com.novacommerce.inventory.api.dto.*; import com.novacommerce.inventory.api.dto.InventoryDtos.*; import com.novacommerce.inventory.api.dto.InventoryRequests.*; import com.novacommerce.inventory.application.InventoryService; import jakarta.validation.Valid; import java.net.URI; import java.util.*; import org.springframework.http.*; import org.springframework.security.oauth2.jwt.Jwt; import org.springframework.security.core.annotation.AuthenticationPrincipal; import org.springframework.web.bind.annotation.*;
@RestController public class InventoryController {private final InventoryService inventory;public InventoryController(InventoryService i){inventory=i;}
 @GetMapping("/api/v1/inventory/variants/{id}") Availability availability(@PathVariable UUID id){return inventory.availability(id);}
 @PostMapping("/api/v1/inventory/availability") List<Availability> availability(@Valid @RequestBody BulkAvailabilityRequest request){return inventory.availability(request.variantIds());}
 @PostMapping("/api/v1/inventory/reservations") ResponseEntity<Reservation> reserve(@RequestHeader(value="Idempotency-Key",required=false)String key,@Valid @RequestBody ReservationRequest request,@AuthenticationPrincipal Jwt jwt){var result=inventory.reserve(key,request,subject(jwt));return result.created()?ResponseEntity.created(URI.create("/api/v1/inventory/reservations/"+result.reservation().id())).body(result.reservation()):ResponseEntity.ok(result.reservation());}
 @GetMapping("/api/v1/inventory/reservations/{id}") Reservation get(@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt){return inventory.get(id,subject(jwt),admin(jwt));}
 @PostMapping("/api/v1/inventory/reservations/{id}/release") Reservation release(@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt){return inventory.release(id,subject(jwt),admin(jwt));}
 @PostMapping("/api/v1/admin/inventory/reservations/{id}/commit") Reservation commit(@PathVariable UUID id){return inventory.commit(id);}
 private static UUID subject(Jwt jwt){try{return UUID.fromString(jwt.getSubject());}catch(Exception e){throw new com.novacommerce.inventory.api.error.InventoryException(HttpStatus.UNAUTHORIZED,"INVALID_SUBJECT","JWT subject must be a UUID");}}
 private static boolean admin(Jwt jwt){List<String> roles=jwt.getClaimAsStringList("roles");return roles!=null&&roles.contains("ADMIN");}
}
