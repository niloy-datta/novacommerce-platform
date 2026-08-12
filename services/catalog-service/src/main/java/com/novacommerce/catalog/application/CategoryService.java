package com.novacommerce.catalog.application;

import com.novacommerce.catalog.api.dto.CatalogDtos.CategoryResponse;
import com.novacommerce.catalog.api.dto.CatalogRequests.CategoryRequest;
import com.novacommerce.catalog.api.error.CatalogException;
import com.novacommerce.catalog.domain.category.Category;
import com.novacommerce.catalog.infrastructure.persistence.CategoryRepository;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryService {
    private final CategoryRepository categories;
    public CategoryService(CategoryRepository categories) { this.categories = categories; }
    @Transactional(readOnly = true) public List<CategoryResponse> publicList() { return categories.findAllByActiveTrueOrderByNameAsc().stream().map(CategoryResponse::from).toList(); }
    @Transactional public CategoryResponse create(CategoryRequest request) { String slug = SlugNormalizer.normalize(request.slug()); if (categories.existsBySlug(slug)) throw duplicate(); Category parent = request.parentId() == null ? null : find(request.parentId()); return CategoryResponse.from(categories.save(new Category(UUID.randomUUID(), request.name(), slug, request.description(), parent))); }
    @Transactional public CategoryResponse update(UUID id, CategoryRequest request) { Category category = find(id); String slug = SlugNormalizer.normalize(request.slug()); if (!category.getSlug().equals(slug) && categories.existsBySlug(slug)) throw duplicate(); Category parent = request.parentId() == null ? null : find(request.parentId()); validateParent(id, parent == null ? null : parent.getId()); category.update(request.name(), slug, request.description(), parent, request.active() == null || request.active()); return CategoryResponse.from(categories.save(category)); }
    public Category find(UUID id) { return categories.findById(id).orElseThrow(() -> new CatalogException(HttpStatus.NOT_FOUND, "CATEGORY_NOT_FOUND", "Category was not found")); }
    public Category findBySlug(String slug) { return categories.findBySlug(SlugNormalizer.normalize(slug)).filter(Category::isActive).orElseThrow(() -> new CatalogException(HttpStatus.NOT_FOUND, "CATEGORY_NOT_FOUND", "Category was not found")); }
    public void validateParent(UUID categoryId, UUID parentId) { if (parentId == null) return; if (categoryId.equals(parentId)) throw new CatalogException(HttpStatus.BAD_REQUEST, "INVALID_CATEGORY_HIERARCHY", "Category cannot be its own parent"); UUID current = parentId; HashSet<UUID> visited = new HashSet<>(); while (current != null && visited.add(current)) { if (current.equals(categoryId)) throw new CatalogException(HttpStatus.BAD_REQUEST, "INVALID_CATEGORY_HIERARCHY", "Category hierarchy would create a cycle"); current = categories.findParentIdById(current).orElse(null); } }
    private CatalogException duplicate() { return new CatalogException(HttpStatus.CONFLICT, "SLUG_ALREADY_EXISTS", "Category slug already exists"); }
}
