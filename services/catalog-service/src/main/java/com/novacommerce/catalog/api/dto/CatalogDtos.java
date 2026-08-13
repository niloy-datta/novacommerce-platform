package com.novacommerce.catalog.api.dto;

import com.novacommerce.catalog.domain.brand.Brand;
import com.novacommerce.catalog.domain.category.Category;
import com.novacommerce.catalog.domain.product.Product;
import com.novacommerce.catalog.domain.product.ProductImage;
import com.novacommerce.catalog.domain.product.ProductVariant;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class CatalogDtos {
    private CatalogDtos() { }
    public record BrandResponse(UUID id, String name, String slug, String description) { public static BrandResponse from(Brand b) { return new BrandResponse(b.getId(), b.getName(), b.getSlug(), b.getDescription()); } }
    public record CategoryResponse(UUID id, String name, String slug, String description, String parentSlug) { public static CategoryResponse from(Category c) { return new CategoryResponse(c.getId(), c.getName(), c.getSlug(), c.getDescription(), c.getParent() == null ? null : c.getParent().getSlug()); } }
    public record PriceResponse(BigDecimal amount, String currency) { }
    public record VariantResponse(UUID id, String sku, String name, Map<String, String> attributes, PriceResponse price, boolean active) { public static VariantResponse from(ProductVariant v) { return new VariantResponse(v.getId(), v.getSku(), v.getName(), v.getAttributes(), new PriceResponse(v.getPriceAmount(), v.getCurrency()), v.isActive()); } }
    public record CheckoutVariantResponse(UUID variantId, UUID productId, String productName, String productSlug, String variantName, String sku, Map<String,String> attributes, PriceResponse price) { public static CheckoutVariantResponse from(ProductVariant v) { Product p=v.getProduct(); return new CheckoutVariantResponse(v.getId(),p.getId(),p.getName(),p.getSlug(),v.getName(),v.getSku(),v.getAttributes(),new PriceResponse(v.getPriceAmount(),v.getCurrency())); } }
    public record ImageResponse(UUID id, String url, String altText, int sortOrder, UUID variantId) { public static ImageResponse from(ProductImage i) { return new ImageResponse(i.getId(), i.getUrl(), i.getAltText(), i.getSortOrder(), i.getVariant() == null ? null : i.getVariant().getId()); } }
    public record ProductSummaryResponse(UUID id, String name, String slug, String shortDescription, BrandResponse brand, PriceResponse startingPrice) { public static ProductSummaryResponse from(Product p) { return new ProductSummaryResponse(p.getId(), p.getName(), p.getSlug(), p.getShortDescription(), p.getBrand() == null ? null : BrandResponse.from(p.getBrand()), p.getVariants().stream().filter(ProductVariant::isActive).map(v -> new PriceResponse(v.getPriceAmount(), v.getCurrency())).min(Comparator.comparing(PriceResponse::amount)).orElse(null)); } }
    public record ProductDetailResponse(UUID id, String name, String slug, String shortDescription, String description, String status, BrandResponse brand, List<CategoryResponse> categories, List<VariantResponse> variants, List<ImageResponse> images, long version) {
        public static ProductDetailResponse from(Product p) {
            return new ProductDetailResponse(p.getId(), p.getName(), p.getSlug(), p.getShortDescription(), p.getDescription(), p.getStatus().name(), p.getBrand() == null ? null : BrandResponse.from(p.getBrand()), p.getCategories().stream().sorted(Comparator.comparing(Category::getName)).map(CategoryResponse::from).toList(), p.getVariants().stream().sorted(Comparator.comparing(ProductVariant::getSku)).map(VariantResponse::from).toList(), p.getImages().stream().sorted(Comparator.comparingInt(ProductImage::getSortOrder)).map(ImageResponse::from).toList(), p.getVersion());
        }
        public static ProductDetailResponse fromPublic(Product p) {
            return new ProductDetailResponse(p.getId(), p.getName(), p.getSlug(), p.getShortDescription(), p.getDescription(), p.getStatus().name(), p.getBrand() == null ? null : BrandResponse.from(p.getBrand()), p.getCategories().stream().sorted(Comparator.comparing(Category::getName)).map(CategoryResponse::from).toList(), p.getVariants().stream().filter(ProductVariant::isActive).sorted(Comparator.comparing(ProductVariant::getSku)).map(VariantResponse::from).toList(), p.getImages().stream().sorted(Comparator.comparingInt(ProductImage::getSortOrder)).map(ImageResponse::from).toList(), p.getVersion());
        }
    }
    public record PageResponse<T>(List<T> items, int page, int size, long totalElements, int totalPages) { }
}
