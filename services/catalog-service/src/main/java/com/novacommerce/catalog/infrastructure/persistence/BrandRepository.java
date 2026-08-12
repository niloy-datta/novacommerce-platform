package com.novacommerce.catalog.infrastructure.persistence;

import com.novacommerce.catalog.domain.brand.Brand;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BrandRepository extends JpaRepository<Brand, UUID> {
    Optional<Brand> findBySlug(String slug);
    List<Brand> findAllByActiveTrueOrderByNameAsc();
    boolean existsBySlug(String slug);
}
