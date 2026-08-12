package com.novacommerce.catalog.infrastructure.persistence;

import com.novacommerce.catalog.domain.product.ProductImage;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductImageRepository extends JpaRepository<ProductImage, UUID> { }
