package com.novacommerce.catalog.infrastructure.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class ProductSearchRepositoryImpl implements ProductSearchRepository {
    @PersistenceContext private EntityManager entityManager;

    @Override
    public SearchResult searchPublic(String query, String category, String brand, BigDecimal minPrice, BigDecimal maxPrice, String sort, int page, int size) {
        boolean postgres = String.valueOf(entityManager.getEntityManagerFactory().getProperties().get("hibernate.dialect")).toLowerCase().contains("postgres");
        StringBuilder where = new StringBuilder(" FROM products p LEFT JOIN brands b ON b.id = p.brand_id WHERE p.status = 'ACTIVE'");
        if (query != null && !query.isBlank()) where.append(postgres ? " AND p.search_vector @@ websearch_to_tsquery('english', :query)" : " AND (LOWER(p.name) LIKE LOWER(:likeQuery) OR LOWER(COALESCE(p.short_description,'')) LIKE LOWER(:likeQuery) OR LOWER(COALESCE(p.description,'')) LIKE LOWER(:likeQuery))");
        if (brand != null && !brand.isBlank()) where.append(" AND LOWER(b.slug) = LOWER(:brand)");
        if (category != null && !category.isBlank()) where.append(" AND EXISTS (SELECT 1 FROM product_categories pc JOIN categories c ON c.id = pc.category_id WHERE pc.product_id = p.id AND LOWER(c.slug) = LOWER(:category) AND c.active = TRUE)");
        if (minPrice != null) where.append(" AND EXISTS (SELECT 1 FROM product_variants vmin WHERE vmin.product_id = p.id AND vmin.active = TRUE AND vmin.price_amount >= :minPrice)");
        if (maxPrice != null) where.append(" AND EXISTS (SELECT 1 FROM product_variants vmax WHERE vmax.product_id = p.id AND vmax.active = TRUE AND vmax.price_amount <= :maxPrice)");
        String order = orderBy(sort, query, postgres);
        var idsQuery = entityManager.createNativeQuery("SELECT p.id" + where + order);
        var countQuery = entityManager.createNativeQuery("SELECT COUNT(*)" + where);
        bind(idsQuery, query, category, brand, minPrice, maxPrice, postgres);
        bind(countQuery, query, category, brand, minPrice, maxPrice, postgres);
        List<UUID> ids = new ArrayList<>();
        for (Object value : idsQuery.setFirstResult(page * size).setMaxResults(size).getResultList()) ids.add(toUuid(value));
        Number total = (Number) countQuery.getSingleResult();
        return new SearchResult(ids, total.longValue());
    }

    private UUID toUuid(Object value) {
        if (value instanceof UUID uuid) return uuid;
        if (value instanceof byte[] bytes && bytes.length == 16) {
            java.nio.ByteBuffer buffer = java.nio.ByteBuffer.wrap(bytes);
            return new UUID(buffer.getLong(), buffer.getLong());
        }
        return UUID.fromString(value.toString());
    }

    private String orderBy(String sort, String query, boolean postgres) {
        String normalized = sort == null || sort.isBlank() ? (query == null || query.isBlank() ? "newest" : "relevance") : sort;
        return switch (normalized) {
            case "relevance" -> postgres && query != null && !query.isBlank() ? " ORDER BY ts_rank_cd(p.search_vector, websearch_to_tsquery('english', :query)) DESC, p.created_at DESC, p.id" : " ORDER BY p.created_at DESC, p.id";
            case "newest" -> " ORDER BY p.created_at DESC, p.id";
            case "price_asc" -> " ORDER BY (SELECT MIN(v.price_amount) FROM product_variants v WHERE v.product_id = p.id AND v.active = TRUE) ASC NULLS LAST, p.id";
            case "price_desc" -> " ORDER BY (SELECT MIN(v.price_amount) FROM product_variants v WHERE v.product_id = p.id AND v.active = TRUE) DESC NULLS LAST, p.id";
            case "name_asc" -> " ORDER BY LOWER(p.name) ASC, p.id";
            case "name_desc" -> " ORDER BY LOWER(p.name) DESC, p.id";
            default -> throw new IllegalArgumentException("Unsupported sort");
        };
    }

    private void bind(jakarta.persistence.Query queryObject, String query, String category, String brand, BigDecimal minPrice, BigDecimal maxPrice, boolean postgres) {
        if (query != null && !query.isBlank()) { if (postgres) queryObject.setParameter("query", query); else queryObject.setParameter("likeQuery", "%" + query.trim() + "%"); }
        if (category != null && !category.isBlank()) queryObject.setParameter("category", category);
        if (brand != null && !brand.isBlank()) queryObject.setParameter("brand", brand);
        if (minPrice != null) queryObject.setParameter("minPrice", minPrice);
        if (maxPrice != null) queryObject.setParameter("maxPrice", maxPrice);
    }
}
