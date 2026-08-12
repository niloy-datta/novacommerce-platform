package com.novacommerce.catalog.api.admin;

import com.novacommerce.catalog.api.dto.CatalogDtos;
import com.novacommerce.catalog.api.dto.CatalogRequests;
import com.novacommerce.catalog.application.BrandService;
import com.novacommerce.catalog.application.CategoryService;
import com.novacommerce.catalog.application.ProductCommandService;
import com.novacommerce.catalog.application.ProductQueryService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/catalog")
public class CatalogAdminController {
    private final BrandService brands; private final CategoryService categories; private final ProductCommandService commands; private final ProductQueryService queries;
    public CatalogAdminController(BrandService brands, CategoryService categories, ProductCommandService commands, ProductQueryService queries) { this.brands = brands; this.categories = categories; this.commands = commands; this.queries = queries; }
    @PostMapping("/brands") public ResponseEntity<CatalogDtos.BrandResponse> createBrand(@Valid @RequestBody CatalogRequests.BrandRequest request) { return ResponseEntity.status(HttpStatus.CREATED).body(brands.create(request)); }
    @PatchMapping("/brands/{id}") public CatalogDtos.BrandResponse updateBrand(@PathVariable UUID id, @Valid @RequestBody CatalogRequests.BrandRequest request) { return brands.update(id, request); }
    @PostMapping("/categories") public ResponseEntity<CatalogDtos.CategoryResponse> createCategory(@Valid @RequestBody CatalogRequests.CategoryRequest request) { return ResponseEntity.status(HttpStatus.CREATED).body(categories.create(request)); }
    @PatchMapping("/categories/{id}") public CatalogDtos.CategoryResponse updateCategory(@PathVariable UUID id, @Valid @RequestBody CatalogRequests.CategoryRequest request) { return categories.update(id, request); }
    @PostMapping("/products") public ResponseEntity<CatalogDtos.ProductDetailResponse> createProduct(@Valid @RequestBody CatalogRequests.ProductCreateRequest request) { return ResponseEntity.status(HttpStatus.CREATED).body(commands.create(request)); }
    @GetMapping("/products/{id}") public CatalogDtos.ProductDetailResponse getProduct(@PathVariable UUID id) { return queries.adminById(id); }
    @PatchMapping("/products/{id}") public CatalogDtos.ProductDetailResponse updateProduct(@PathVariable UUID id, @Valid @RequestBody CatalogRequests.ProductPatchRequest request) { return commands.update(id, request); }
    @PostMapping("/products/{id}/variants") public ResponseEntity<CatalogDtos.ProductDetailResponse> addVariant(@PathVariable UUID id, @Valid @RequestBody CatalogRequests.VariantRequest request) { return ResponseEntity.status(HttpStatus.CREATED).body(commands.addVariant(id, request)); }
    @PatchMapping("/variants/{id}") public CatalogDtos.ProductDetailResponse updateVariant(@PathVariable UUID id, @Valid @RequestBody CatalogRequests.VariantPatchRequest request) { return commands.updateVariant(id, request); }
    @PostMapping("/products/{id}/images") public ResponseEntity<CatalogDtos.ProductDetailResponse> addImage(@PathVariable UUID id, @Valid @RequestBody CatalogRequests.ImageRequest request) { return ResponseEntity.status(HttpStatus.CREATED).body(commands.addImage(id, request)); }
    @DeleteMapping("/images/{id}") public ResponseEntity<Void> deleteImage(@PathVariable UUID id) { commands.deleteImage(id); return ResponseEntity.noContent().build(); }
}
