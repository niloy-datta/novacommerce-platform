package com.novacommerce.catalog.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class CatalogRequests {
    private CatalogRequests() { }
    public record BrandRequest(@NotBlank @Size(max = 160) String name, @NotBlank @Size(max = 180) String slug, @Size(max = 2000) String description, Boolean active) { }
    public record CategoryRequest(@NotBlank @Size(max = 160) String name, @NotBlank @Size(max = 180) String slug, @Size(max = 2000) String description, UUID parentId, Boolean active) { }
    public record ProductCreateRequest(@NotBlank @Size(max = 200) String name, @NotBlank @Size(max = 220) String slug, @Size(max = 500) String shortDescription, @Size(max = 10000) String description, UUID brandId, List<UUID> categoryIds, String status) { }
    public record ProductPatchRequest(@NotBlank @Size(max = 200) String name, @NotBlank @Size(max = 220) String slug, @Size(max = 500) String shortDescription, @Size(max = 10000) String description, UUID brandId, List<UUID> categoryIds, String status, Long version) { }
    public record VariantRequest(@NotBlank @Size(max = 100) String sku, @NotBlank @Size(max = 200) String name, Map<String, String> attributes, @NotNull @DecimalMin("0.00") BigDecimal priceAmount, @NotBlank String currency, Boolean active) { }
    public record VariantPatchRequest(@NotBlank @Size(max = 200) String name, Map<String, String> attributes, @NotNull @DecimalMin("0.00") BigDecimal priceAmount, @NotBlank String currency, Boolean active, Long version) { }
    public record ImageRequest(@NotBlank @Size(max = 2000) String url, @Size(max = 300) String altText, Integer sortOrder, UUID variantId) { }
}
