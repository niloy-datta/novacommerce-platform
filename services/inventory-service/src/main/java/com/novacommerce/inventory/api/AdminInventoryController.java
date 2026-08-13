package com.novacommerce.inventory.api;
import com.novacommerce.inventory.api.dto.InventoryDtos.*; import com.novacommerce.inventory.api.dto.InventoryRequests.AdjustmentRequest; import com.novacommerce.inventory.application.InventoryService; import jakarta.validation.Valid; import java.util.*; import org.springframework.security.oauth2.jwt.Jwt; import org.springframework.security.core.annotation.AuthenticationPrincipal; import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/admin/inventory") public class AdminInventoryController {private final InventoryService inventory;public AdminInventoryController(InventoryService i){inventory=i;}
 @GetMapping("/{id}") AdminInventory get(@PathVariable UUID id){return inventory.admin(id);}
 @PostMapping("/{id}/adjustments") AdminInventory adjust(@PathVariable UUID id,@Valid @RequestBody AdjustmentRequest request,@AuthenticationPrincipal Jwt jwt){return inventory.adjust(id,request.quantityDelta(),request.reason(),UUID.fromString(jwt.getSubject()));}
 @GetMapping("/{id}/movements") List<Movement> movements(@PathVariable UUID id){return inventory.movements(id);}
}
