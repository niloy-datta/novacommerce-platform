package com.novacommerce.auth.api.dto;

public record CsrfResponse(String token, String headerName) { }
