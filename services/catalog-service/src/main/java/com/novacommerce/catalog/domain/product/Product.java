package com.novacommerce.catalog.domain.product;

import com.novacommerce.catalog.domain.brand.Brand;
import com.novacommerce.catalog.domain.category.Category;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "products")
public class Product {
    @Id private UUID id;
    @Column(nullable = false, length = 200) private String name;
    @Column(nullable = false, unique = true, length = 220) private String slug;
    @Column(name = "short_description", length = 500) private String shortDescription;
    @Column(columnDefinition = "text") private String description;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "brand_id") private Brand brand;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private ProductStatus status;
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "product_categories", joinColumns = @JoinColumn(name = "product_id"), inverseJoinColumns = @JoinColumn(name = "category_id"))
    private Set<Category> categories = new LinkedHashSet<>();
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true) private Set<ProductVariant> variants = new LinkedHashSet<>();
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true) private Set<ProductImage> images = new LinkedHashSet<>();
    @Version private long version;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    protected Product() { }
    public Product(UUID id, String name, String slug, String shortDescription, String description, Brand brand) { this.id = id; this.name = required(name); this.slug = required(slug); this.shortDescription = shortDescription; this.description = description; this.brand = brand; this.status = ProductStatus.DRAFT; }
    public void updateBasicInformation(String name, String slug, String shortDescription, String description, Brand brand) { this.name = required(name); this.slug = required(slug); this.shortDescription = shortDescription; this.description = description; this.brand = brand; }
    public void replaceCategories(Set<Category> categories) { this.categories.clear(); if (categories != null) this.categories.addAll(categories); }
    public void activate() { if (variants.stream().noneMatch(ProductVariant::isActive)) throw new IllegalStateException("Product requires an active variant before activation"); status = ProductStatus.ACTIVE; }
    public void archive() { status = ProductStatus.ARCHIVED; }
    public void returnToDraft() { status = ProductStatus.DRAFT; }
    public void addVariant(ProductVariant variant) { variant.attachTo(this); variants.add(variant); }
    public void addImage(ProductImage image) { image.attachTo(this); images.add(image); }
    public void removeImage(ProductImage image) { images.remove(image); }
    @PrePersist void onCreate() { createdAt = updatedAt = Instant.now(); } @PreUpdate void onUpdate() { updatedAt = Instant.now(); }
    private static String required(String value) { if (value == null || value.isBlank()) throw new IllegalArgumentException("Required value is missing"); return value.trim(); }
    public UUID getId() { return id; } public String getName() { return name; } public String getSlug() { return slug; }
    public String getShortDescription() { return shortDescription; } public String getDescription() { return description; } public Brand getBrand() { return brand; }
    public ProductStatus getStatus() { return status; } public Set<Category> getCategories() { return Set.copyOf(categories); } public Set<ProductVariant> getVariants() { return Set.copyOf(variants); }
    public Set<ProductImage> getImages() { return Set.copyOf(images); } public long getVersion() { return version; } public Instant getCreatedAt() { return createdAt; } public Instant getUpdatedAt() { return updatedAt; }
}
