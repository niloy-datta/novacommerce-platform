package com.novacommerce.auth.infrastructure.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.stereotype.Component;

@Component
public class CookieBearerTokenResolver implements BearerTokenResolver {
    public static final String ACCESS_COOKIE = "NC_ACCESS";

    @Override
    public String resolve(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie cookie : cookies) if (ACCESS_COOKIE.equals(cookie.getName())) return cookie.getValue();
        return null;
    }
}
