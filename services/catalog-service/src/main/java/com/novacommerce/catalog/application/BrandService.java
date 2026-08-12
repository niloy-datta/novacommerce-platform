package com.novacommerce.catalog.application;

import com.novacommerce.catalog.api.dto.CatalogRequests.BrandRequest;
import com.novacommerce.catalog.api.dto.CatalogDtos.BrandResponse;
import com.novacommerce.catalog.api.error.CatalogException;
import com.novacommerce.catalog.domain.brand.Brand;
import com.novacommerce.catalog.infrastructure.persistence.BrandRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BrandService {
    private final BrandRepository brands;
    public BrandService(BrandRepository brands) { this.brands = brands; }
    @Transactional(readOnly = true) public List<BrandResponse> publicList() { return brands.findAllByActiveTrueOrderByNameAsc().stream().map(BrandResponse::from).toList(); }
    @Transactional public BrandResponse create(BrandRequest request) { String slug = SlugNormalizer.normalize(request.slug()); if (brands.existsBySlug(slug)) throw duplicate(); return BrandResponse.from(brands.save(new Brand(UUID.randomUUID(), request.name(), slug, request.description()))); }
    @Transactional public BrandResponse update(UUID id, BrandRequest request) { Brand brand = find(id); String slug = SlugNormalizer.normalize(request.slug()); if (!brand.getSlug().equals(slug) && brands.existsBySlug(slug)) throw duplicate(); brand.update(request.name(), slug, request.description(), request.active() == null || request.active()); return BrandResponse.from(brands.save(brand)); }
    public Brand find(UUID id) { return brands.findById(id).orElseThrow(() -> new CatalogException(HttpStatus.NOT_FOUND, "BRAND_NOT_FOUND", "Brand was not found")); }
    public Brand findOptional(UUID id) { return id == null ? null : find(id); }
    private CatalogException duplicate() { return new CatalogException(HttpStatus.CONFLICT, "SLUG_ALREADY_EXISTS", "Brand slug already exists"); }
}
