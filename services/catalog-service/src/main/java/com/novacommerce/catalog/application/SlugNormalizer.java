package com.novacommerce.catalog.application;

import java.text.Normalizer;
import java.util.Locale;

public final class SlugNormalizer {
    private SlugNormalizer() { }
    public static String normalize(String value) {
        if (value == null) throw new IllegalArgumentException("Slug cannot be null");
        String ascii = Normalizer.normalize(value.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String slug = ascii.replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        if (slug.isBlank()) throw new IllegalArgumentException("Slug cannot be empty");
        return slug;
    }
}
