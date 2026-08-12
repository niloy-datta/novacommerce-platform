package com.novacommerce.catalog.domain.product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "product_variants")
public class ProductVariant {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "product_id", nullable = false) private Product product;
    @Column(nullable = false, unique = true, length = 100) private String sku;
    @Column(nullable = false, length = 200) private String name;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable = false, columnDefinition = "jsonb") private Map<String, String> attributes = new LinkedHashMap<>();
    @Column(name = "price_amount", nullable = false, precision = 19, scale = 2) private BigDecimal priceAmount;
    @Column(nullable = false, length = 3) private String currency;
    @Column(nullable = false) private boolean active;
    @Version private long version;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    protected ProductVariant() { }
    public ProductVariant(UUID id, String sku, String name, Map<String, String> attributes, BigDecimal price, String currency) {
        this.id = id; this.sku = required(sku).toUpperCase(); this.name = required(name); this.attributes = attributes == null ? new LinkedHashMap<>() : new LinkedHashMap<>(attributes); changePrice(price, currency); this.active = true;
    }
    void attachTo(Product product) { if (this.product != null && !this.product.getId().equals(product.getId())) throw new IllegalStateException("Variant already belongs to another product"); this.product = product; }
    public void update(String name, Map<String, String> attributes, BigDecimal price, String currency, boolean active) { this.name = required(name); this.attributes = attributes == null ? new LinkedHashMap<>() : new LinkedHashMap<>(attributes); changePrice(price, currency); this.active = active; }
    private void changePrice(BigDecimal price, String currencyCode) { if (price == null || price.signum() < 0) throw new IllegalArgumentException("Price cannot be negative"); String code = required(currencyCode).toUpperCase(); Currency.getInstance(code); this.priceAmount = price; this.currency = code; }
    @PrePersist void onCreate() { createdAt = updatedAt = Instant.now(); } @PreUpdate void onUpdate() { updatedAt = Instant.now(); }
    private static String required(String value) { if (value == null || value.isBlank()) throw new IllegalArgumentException("Required value is missing"); return value.trim(); }
    public UUID getId() { return id; } public Product getProduct() { return product; } public String getSku() { return sku; } public String getName() { return name; }
    public Map<String, String> getAttributes() { return Map.copyOf(attributes); } public BigDecimal getPriceAmount() { return priceAmount; } public String getCurrency() { return currency; }
    public boolean isActive() { return active; } public long getVersion() { return version; } public Instant getCreatedAt() { return createdAt; } public Instant getUpdatedAt() { return updatedAt; }
}
