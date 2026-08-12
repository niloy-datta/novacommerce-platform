package com.novacommerce.catalog.api.error;

import java.time.Instant;
import java.util.Map;

public record CatalogApiError(Instant timestamp, int status, String code, String message, String path, Map<String, String> fieldErrors) { }
