package com.novacommerce.inventory.infrastructure.persistence;
import com.novacommerce.inventory.domain.inventory.InventoryMovement; import java.util.*; import org.springframework.data.domain.Pageable; import org.springframework.data.jpa.repository.JpaRepository;
public interface InventoryMovementRepository extends JpaRepository<InventoryMovement,UUID>{List<InventoryMovement> findByVariantIdOrderByCreatedAtDesc(UUID id,Pageable pageable);}
