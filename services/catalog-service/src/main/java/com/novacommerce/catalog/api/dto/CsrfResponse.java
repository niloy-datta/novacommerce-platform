package com.novacommerce.catalog.api.dto;

public record CsrfResponse(String token, String headerName) { }
