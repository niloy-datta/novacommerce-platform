package com.novacommerce.catalog.infrastructure.persistence;

import com.novacommerce.catalog.domain.product.Product;
import com.novacommerce.catalog.domain.product.ProductStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, UUID>, ProductSearchRepository {
    Optional<Product> findBySlug(String slug);
    Optional<Product> findBySlugAndStatus(String slug, ProductStatus status);
    boolean existsBySlug(String slug);
}
