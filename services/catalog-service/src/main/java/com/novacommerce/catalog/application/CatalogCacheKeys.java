package com.novacommerce.catalog.application;

public final class CatalogCacheKeys {
    private CatalogCacheKeys() { }
    public static String product(String slug) { return "catalog:product:" + slug; }
}
