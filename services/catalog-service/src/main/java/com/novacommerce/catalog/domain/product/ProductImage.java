package com.novacommerce.catalog.domain.product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "product_images")
public class ProductImage {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "product_id", nullable = false) private Product product;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "variant_id") private ProductVariant variant;
    @Column(nullable = false, length = 2000) private String url;
    @Column(name = "alt_text", length = 300) private String altText;
    @Column(name = "sort_order", nullable = false) private int sortOrder;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    protected ProductImage() { }
    public ProductImage(UUID id, String url, String altText, int sortOrder, ProductVariant variant) { if (sortOrder < 0) throw new IllegalArgumentException("Sort order cannot be negative"); this.id = id; this.url = required(url); this.altText = altText; this.sortOrder = sortOrder; this.variant = variant; }
    void attachTo(Product product) { if (variant != null && variant.getProduct() != null && !variant.getProduct().getId().equals(product.getId())) throw new IllegalArgumentException("Image variant belongs to another product"); this.product = product; }
    @PrePersist void onCreate() { createdAt = Instant.now(); }
    private static String required(String value) { if (value == null || value.isBlank()) throw new IllegalArgumentException("Image URL is required"); return value.trim(); }
    public UUID getId() { return id; } public Product getProduct() { return product; } public String getUrl() { return url; } public String getAltText() { return altText; } public int getSortOrder() { return sortOrder; } public ProductVariant getVariant() { return variant; } public Instant getCreatedAt() { return createdAt; }
}
