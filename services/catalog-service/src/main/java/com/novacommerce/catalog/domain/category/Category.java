package com.novacommerce.catalog.domain.category;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "categories")
public class Category {
    @Id private UUID id;
    @Column(nullable = false, length = 160) private String name;
    @Column(nullable = false, unique = true, length = 180) private String slug;
    @Column(columnDefinition = "text") private String description;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "parent_id") private Category parent;
    @Column(nullable = false) private boolean active;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    protected Category() { }
    public Category(UUID id, String name, String slug, String description, Category parent) { this.id = id; this.name = required(name); this.slug = required(slug); this.description = description; this.parent = parent; this.active = true; }
    public void update(String name, String slug, String description, Category parent, boolean active) { if (parent != null && parent.getId().equals(id)) throw new IllegalArgumentException("Category cannot be its own parent"); this.name = required(name); this.slug = required(slug); this.description = description; this.parent = parent; this.active = active; }
    @PrePersist void onCreate() { createdAt = updatedAt = Instant.now(); } @PreUpdate void onUpdate() { updatedAt = Instant.now(); }
    private static String required(String value) { if (value == null || value.isBlank()) throw new IllegalArgumentException("Required value is missing"); return value.trim(); }
    public UUID getId() { return id; } public String getName() { return name; } public String getSlug() { return slug; }
    public String getDescription() { return description; } public Category getParent() { return parent; } public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; } public Instant getUpdatedAt() { return updatedAt; }
}
