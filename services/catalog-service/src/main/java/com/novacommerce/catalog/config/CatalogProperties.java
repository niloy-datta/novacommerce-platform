package com.novacommerce.catalog.config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "catalog")
public class CatalogProperties {
    private final Auth auth = new Auth();
    private final Cache cache = new Cache();
    private List<String> allowedOrigins = new ArrayList<>();

    public Auth getAuth() { return auth; }
    public Cache getCache() { return cache; }
    public List<String> getAllowedOrigins() { return allowedOrigins; }
    public void setAllowedOrigins(List<String> allowedOrigins) { this.allowedOrigins = allowedOrigins; }

    public static class Auth {
        private String issuer;
        private String audience;
        private String jwksUri;
        public String getIssuer() { return issuer; }
        public void setIssuer(String issuer) { this.issuer = issuer; }
        public String getAudience() { return audience; }
        public void setAudience(String audience) { this.audience = audience; }
        public String getJwksUri() { return jwksUri; }
        public void setJwksUri(String jwksUri) { this.jwksUri = jwksUri; }
    }

    public static class Cache {
        private Duration productTtl = Duration.ofMinutes(10);
        public Duration getProductTtl() { return productTtl; }
        public void setProductTtl(Duration productTtl) { this.productTtl = productTtl; }
    }
}
