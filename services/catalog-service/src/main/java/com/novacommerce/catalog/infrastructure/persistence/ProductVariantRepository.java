package com.novacommerce.catalog.infrastructure.persistence;

import com.novacommerce.catalog.domain.product.ProductVariant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, UUID> {
    boolean existsBySkuIgnoreCase(String sku);
    Optional<ProductVariant> findBySkuIgnoreCase(String sku);
}
