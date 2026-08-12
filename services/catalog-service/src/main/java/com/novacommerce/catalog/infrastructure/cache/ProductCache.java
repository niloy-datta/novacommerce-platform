package com.novacommerce.catalog.infrastructure.cache;

import com.novacommerce.catalog.api.dto.CatalogDtos.ProductDetailResponse;
import com.novacommerce.catalog.application.CatalogCacheKeys;
import com.novacommerce.catalog.config.CatalogProperties;
import java.time.Duration;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.ObjectMapper;

@Component
public class ProductCache {
    private static final Logger LOG = LoggerFactory.getLogger(ProductCache.class);
    private final StringRedisTemplate redis; private final ObjectMapper mapper; private final Duration ttl;
    public ProductCache(StringRedisTemplate redis, ObjectMapper mapper, CatalogProperties properties) { this.redis = redis; this.mapper = mapper; this.ttl = properties.getCache().getProductTtl(); }
    public Optional<ProductDetailResponse> get(String slug) { try { String value = redis.opsForValue().get(CatalogCacheKeys.product(slug)); return value == null ? Optional.empty() : Optional.of(mapper.readValue(value, ProductDetailResponse.class)); } catch (Exception ex) { LOG.warn("Catalog product cache read unavailable; using PostgreSQL"); return Optional.empty(); } }
    public void put(String slug, ProductDetailResponse value) { try { redis.opsForValue().set(CatalogCacheKeys.product(slug), mapper.writeValueAsString(value), ttl); } catch (Exception ex) { LOG.warn("Catalog product cache write unavailable"); } }
    public void evict(String slug) { if (slug == null || slug.isBlank()) return; try { redis.delete(CatalogCacheKeys.product(slug)); } catch (Exception ex) { LOG.warn("Catalog product cache eviction unavailable"); } }
    public void evictAfterCommit(String slug) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) { evict(slug); return; }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { evict(slug); }
        });
    }
}
