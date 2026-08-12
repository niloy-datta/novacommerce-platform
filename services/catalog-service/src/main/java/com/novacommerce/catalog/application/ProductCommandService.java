package com.novacommerce.catalog.application;

import com.novacommerce.catalog.api.dto.CatalogDtos.ProductDetailResponse;
import com.novacommerce.catalog.api.dto.CatalogRequests;
import com.novacommerce.catalog.api.error.CatalogException;
import com.novacommerce.catalog.domain.category.Category;
import com.novacommerce.catalog.domain.product.Product;
import com.novacommerce.catalog.domain.product.ProductImage;
import com.novacommerce.catalog.domain.product.ProductStatus;
import com.novacommerce.catalog.domain.product.ProductVariant;
import com.novacommerce.catalog.infrastructure.cache.ProductCache;
import com.novacommerce.catalog.infrastructure.persistence.CategoryRepository;
import com.novacommerce.catalog.infrastructure.persistence.ProductImageRepository;
import com.novacommerce.catalog.infrastructure.persistence.ProductRepository;
import com.novacommerce.catalog.infrastructure.persistence.ProductVariantRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductCommandService {
    private final ProductRepository products; private final ProductVariantRepository variants; private final ProductImageRepository images; private final CategoryRepository categories; private final BrandService brands; private final CategoryService categoryService; private final ProductCache cache;
    public ProductCommandService(ProductRepository products, ProductVariantRepository variants, ProductImageRepository images, CategoryRepository categories, BrandService brands, CategoryService categoryService, ProductCache cache) { this.products = products; this.variants = variants; this.images = images; this.categories = categories; this.brands = brands; this.categoryService = categoryService; this.cache = cache; }

    @Transactional
    public ProductDetailResponse create(CatalogRequests.ProductCreateRequest request) {
        String slug = SlugNormalizer.normalize(request.slug()); if (products.existsBySlug(slug)) throw duplicateSlug();
        Product product = new Product(UUID.randomUUID(), request.name(), slug, request.shortDescription(), request.description(), brands.findOptional(request.brandId()));
        product.replaceCategories(resolveCategories(request.categoryIds()));
        if (request.status() != null && !request.status().equalsIgnoreCase("DRAFT")) throw new CatalogException(HttpStatus.BAD_REQUEST, "INVALID_PRODUCT_STATE", "Products start as DRAFT and require an active variant before activation");
        return ProductDetailResponse.from(products.save(product));
    }

    @Transactional
    public ProductDetailResponse update(UUID id, CatalogRequests.ProductPatchRequest request) {
        Product product = find(id); if (request.version() != null && request.version() != product.getVersion()) throw new CatalogException(HttpStatus.CONFLICT, "CONCURRENT_MODIFICATION", "The product version is stale");
        String slug = SlugNormalizer.normalize(request.slug()); if (!product.getSlug().equals(slug) && products.existsBySlug(slug)) throw duplicateSlug();
        String oldSlug = product.getSlug(); product.updateBasicInformation(request.name(), slug, request.shortDescription(), request.description(), brands.findOptional(request.brandId()));
        if (request.categoryIds() != null) product.replaceCategories(resolveCategories(request.categoryIds()));
        if (request.status() != null) changeStatus(product, request.status());
        Product saved;
        try { saved = products.saveAndFlush(product); } catch (ObjectOptimisticLockingFailureException ex) { throw new CatalogException(HttpStatus.CONFLICT, "CONCURRENT_MODIFICATION", "The product was changed by another request"); }
        cache.evictAfterCommit(oldSlug); cache.evictAfterCommit(saved.getSlug()); return ProductDetailResponse.from(saved);
    }

    @Transactional
    public ProductDetailResponse addVariant(UUID productId, CatalogRequests.VariantRequest request) {
        Product product = find(productId); if (variants.existsBySkuIgnoreCase(request.sku())) throw new CatalogException(HttpStatus.CONFLICT, "SKU_ALREADY_EXISTS", "SKU already exists");
        ProductVariant variant = new ProductVariant(UUID.randomUUID(), request.sku(), request.name(), request.attributes(), request.priceAmount(), request.currency()); if (request.active() != null && !request.active()) variant.update(request.name(), request.attributes(), request.priceAmount(), request.currency(), false); product.addVariant(variant); products.save(product); cache.evictAfterCommit(product.getSlug()); return ProductDetailResponse.from(product);
    }

    @Transactional
    public ProductDetailResponse updateVariant(UUID variantId, CatalogRequests.VariantPatchRequest request) {
        ProductVariant variant = variants.findById(variantId).orElseThrow(() -> new CatalogException(HttpStatus.NOT_FOUND, "VARIANT_NOT_FOUND", "Variant was not found")); if (request.version() != null && request.version() != variant.getVersion()) throw new CatalogException(HttpStatus.CONFLICT, "CONCURRENT_MODIFICATION", "The variant version is stale");
        String slug = variant.getProduct().getSlug(); variant.update(request.name(), request.attributes(), request.priceAmount(), request.currency(), request.active() == null || request.active()); variants.saveAndFlush(variant); cache.evictAfterCommit(slug); return ProductDetailResponse.from(variant.getProduct());
    }

    @Transactional
    public ProductDetailResponse addImage(UUID productId, CatalogRequests.ImageRequest request) {
        Product product = find(productId); ProductVariant variant = request.variantId() == null ? null : variants.findById(request.variantId()).orElseThrow(() -> new CatalogException(HttpStatus.NOT_FOUND, "VARIANT_NOT_FOUND", "Variant was not found")); if (variant != null && !variant.getProduct().getId().equals(productId)) throw new CatalogException(HttpStatus.BAD_REQUEST, "INVALID_IMAGE_VARIANT", "Image variant does not belong to product");
        product.addImage(new ProductImage(UUID.randomUUID(), request.url(), request.altText(), request.sortOrder() == null ? 0 : request.sortOrder(), variant)); products.save(product); cache.evictAfterCommit(product.getSlug()); return ProductDetailResponse.from(product);
    }

    @Transactional
    public void deleteImage(UUID imageId) { ProductImage image = images.findById(imageId).orElseThrow(() -> new CatalogException(HttpStatus.NOT_FOUND, "IMAGE_NOT_FOUND", "Image was not found")); String slug = image.getProduct().getSlug(); images.delete(image); cache.evictAfterCommit(slug); }
    private Product find(UUID id) { return products.findById(id).orElseThrow(() -> new CatalogException(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND", "Product was not found")); }
    private void changeStatus(Product product, String raw) { ProductStatus status; try { status = ProductStatus.valueOf(raw.toUpperCase()); } catch (IllegalArgumentException ex) { throw new CatalogException(HttpStatus.BAD_REQUEST, "INVALID_PRODUCT_STATE", "Unknown product state"); } if (status == ProductStatus.ACTIVE) product.activate(); else if (status == ProductStatus.ARCHIVED) product.archive(); else product.returnToDraft(); }
    private HashSet<Category> resolveCategories(List<UUID> ids) { if (ids == null) return new HashSet<>(); if (ids.size() != ids.stream().distinct().count()) throw new CatalogException(HttpStatus.BAD_REQUEST, "DUPLICATE_CATEGORY", "Duplicate category IDs are not allowed"); List<Category> found = categories.findAllById(ids); if (found.size() != ids.size()) throw new CatalogException(HttpStatus.NOT_FOUND, "CATEGORY_NOT_FOUND", "One or more categories were not found"); return new HashSet<>(found); }
    private CatalogException duplicateSlug() { return new CatalogException(HttpStatus.CONFLICT, "SLUG_ALREADY_EXISTS", "Product slug already exists"); }
}
