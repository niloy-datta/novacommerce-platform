package com.novacommerce.catalog.infrastructure.persistence;

import com.novacommerce.catalog.domain.product.ProductVariant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
import java.util.List;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, UUID> {
    boolean existsBySkuIgnoreCase(String sku);
    Optional<ProductVariant> findBySkuIgnoreCase(String sku);
    @Query("select v from ProductVariant v join fetch v.product p where v.id in :ids and v.active=true and p.status=com.novacommerce.catalog.domain.product.ProductStatus.ACTIVE")
    List<ProductVariant> findSellableByIds(@Param("ids") Collection<UUID> ids);
}
