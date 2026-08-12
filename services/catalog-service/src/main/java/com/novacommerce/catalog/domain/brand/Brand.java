package com.novacommerce.catalog.domain.brand;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "brands")
public class Brand {
    @Id private UUID id;
    @Column(nullable = false, length = 160) private String name;
    @Column(nullable = false, unique = true, length = 180) private String slug;
    @Column(columnDefinition = "text") private String description;
    @Column(nullable = false) private boolean active;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    protected Brand() { }
    public Brand(UUID id, String name, String slug, String description) { this.id = id; this.name = required(name); this.slug = required(slug); this.description = description; this.active = true; }
    public void update(String name, String slug, String description, boolean active) { this.name = required(name); this.slug = required(slug); this.description = description; this.active = active; }
    @PrePersist void onCreate() { createdAt = updatedAt = Instant.now(); }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }
    private static String required(String value) { if (value == null || value.isBlank()) throw new IllegalArgumentException("Required value is missing"); return value.trim(); }
    public UUID getId() { return id; } public String getName() { return name; } public String getSlug() { return slug; }
    public String getDescription() { return description; } public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; } public Instant getUpdatedAt() { return updatedAt; }
}
