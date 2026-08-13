package com.novacommerce.catalog.api.publicapi;

import com.novacommerce.catalog.api.dto.CatalogDtos;
import com.novacommerce.catalog.application.BrandService;
import com.novacommerce.catalog.application.CategoryService;
import com.novacommerce.catalog.application.ProductQueryService;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class CatalogPublicController {
    private final ProductQueryService products; private final CategoryService categories; private final BrandService brands;
    public CatalogPublicController(ProductQueryService products, CategoryService categories, BrandService brands) { this.products = products; this.categories = categories; this.brands = brands; }
    @GetMapping("/products")
    public CatalogDtos.PageResponse<CatalogDtos.ProductSummaryResponse> products(@RequestParam(required = false) String q, @RequestParam(required = false) String category, @RequestParam(required = false) String brand, @RequestParam(required = false) BigDecimal minPrice, @RequestParam(required = false) BigDecimal maxPrice, @RequestParam(required = false) String sort, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) { return products.search(q, category, brand, minPrice, maxPrice, sort, page, size); }
    @GetMapping("/products/{slug}") public CatalogDtos.ProductDetailResponse product(@PathVariable String slug) { return products.bySlug(slug); }
    @GetMapping("/categories") public List<CatalogDtos.CategoryResponse> categories() { return categories.publicList(); }
    @GetMapping("/categories/{slug}") public CatalogDtos.CategoryResponse category(@PathVariable String slug) { return CatalogDtos.CategoryResponse.from(categories.findBySlug(slug)); }
    @GetMapping("/brands") public List<CatalogDtos.BrandResponse> brands() { return brands.publicList(); }
    @GetMapping("/catalog/variants") public List<CatalogDtos.CheckoutVariantResponse> checkoutVariants(@RequestParam("ids") List<UUID> ids) { return products.checkoutVariants(ids); }
}
