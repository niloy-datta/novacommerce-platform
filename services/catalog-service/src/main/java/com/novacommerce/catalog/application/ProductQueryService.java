package com.novacommerce.catalog.application;

import com.novacommerce.catalog.api.dto.CatalogDtos;
import com.novacommerce.catalog.api.dto.CatalogDtos.ProductDetailResponse;
import com.novacommerce.catalog.api.error.CatalogException;
import com.novacommerce.catalog.domain.product.Product;
import com.novacommerce.catalog.domain.product.ProductStatus;
import com.novacommerce.catalog.infrastructure.cache.ProductCache;
import com.novacommerce.catalog.infrastructure.persistence.ProductRepository;
import com.novacommerce.catalog.infrastructure.persistence.ProductVariantRepository;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductQueryService {
    private final ProductRepository products; private final ProductVariantRepository variants; private final ProductCache cache;
    public ProductQueryService(ProductRepository products, ProductVariantRepository variants, ProductCache cache) { this.products = products; this.variants = variants; this.cache = cache; }
    @Transactional(readOnly=true) public List<CatalogDtos.CheckoutVariantResponse> checkoutVariants(List<java.util.UUID> ids) { if(ids==null||ids.isEmpty()||ids.size()>50||new java.util.HashSet<>(ids).size()!=ids.size()) throw new CatalogException(HttpStatus.BAD_REQUEST,"INVALID_VARIANT_BATCH","Provide 1 to 50 unique variant IDs"); return variants.findSellableByIds(ids).stream().sorted(Comparator.comparing(v->v.getId().toString())).map(CatalogDtos.CheckoutVariantResponse::from).toList(); }
    @Transactional(readOnly = true)
    public CatalogDtos.PageResponse<CatalogDtos.ProductSummaryResponse> search(String query, String category, String brand, BigDecimal minPrice, BigDecimal maxPrice, String sort, int page, int size) {
        if (page < 0 || size < 1 || size > 100) throw new CatalogException(HttpStatus.BAD_REQUEST, "INVALID_PAGINATION", "Page must be non-negative and size must be between 1 and 100");
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) throw new CatalogException(HttpStatus.BAD_REQUEST, "INVALID_PRICE_RANGE", "Minimum price cannot exceed maximum price");
        var result = products.searchPublic(query, category, brand, minPrice, maxPrice, sort, page, size);
        Map<java.util.UUID, Product> byId = new HashMap<>(); products.findAllById(result.ids()).forEach(p -> byId.put(p.getId(), p));
        List<CatalogDtos.ProductSummaryResponse> items = result.ids().stream().map(byId::get).filter(java.util.Objects::nonNull).map(CatalogDtos.ProductSummaryResponse::from).toList();
        return new CatalogDtos.PageResponse<>(items, page, size, result.totalElements(), (int) Math.ceil(result.totalElements() / (double) size));
    }
    @Transactional(readOnly = true)
    public ProductDetailResponse bySlug(String slug) {
        String normalized = SlugNormalizer.normalize(slug);
        var cached = cache.get(normalized); if (cached.isPresent()) return cached.get();
        Product product = products.findBySlugAndStatus(normalized, ProductStatus.ACTIVE).orElseThrow(() -> new CatalogException(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND", "Product was not found"));
        ProductDetailResponse response = ProductDetailResponse.fromPublic(product); cache.put(normalized, response); return response;
    }
    @Transactional(readOnly = true)
    public ProductDetailResponse adminById(java.util.UUID id) { return ProductDetailResponse.from(products.findById(id).orElseThrow(() -> new CatalogException(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND", "Product was not found"))); }
}
