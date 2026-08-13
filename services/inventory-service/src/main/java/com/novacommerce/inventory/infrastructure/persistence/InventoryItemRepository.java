package com.novacommerce.inventory.infrastructure.persistence;
import com.novacommerce.inventory.domain.inventory.InventoryItem; import jakarta.persistence.LockModeType; import java.util.*; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param;
public interface InventoryItemRepository extends JpaRepository<InventoryItem,UUID>{
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select i from InventoryItem i where i.variantId in :ids order by i.variantId") List<InventoryItem> lockAllOrdered(@Param("ids") Collection<UUID> ids);
 List<InventoryItem> findAllByVariantIdIn(Collection<UUID> ids);
}
