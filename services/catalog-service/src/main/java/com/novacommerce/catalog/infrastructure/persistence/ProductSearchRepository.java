package com.novacommerce.catalog.infrastructure.persistence;

import java.math.BigDecimal;
import java.util.List;

public interface ProductSearchRepository {
    SearchResult searchPublic(String query, String category, String brand, BigDecimal minPrice, BigDecimal maxPrice, String sort, int page, int size);

    record SearchResult(List<java.util.UUID> ids, long totalElements) { }
}
