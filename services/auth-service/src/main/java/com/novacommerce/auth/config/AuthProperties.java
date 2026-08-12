package com.novacommerce.auth.config;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auth")
public class AuthProperties {
    private final Jwt jwt = new Jwt();
    private final Cookie cookie = new Cookie();
    private List<String> allowedOrigins = new ArrayList<>();

    public Jwt getJwt() { return jwt; }
    public Cookie getCookie() { return cookie; }
    public List<String> getAllowedOrigins() { return allowedOrigins; }
    public void setAllowedOrigins(List<String> allowedOrigins) { this.allowedOrigins = allowedOrigins; }

    public static class Jwt {
        private Path privateKeyPath;
        private Path publicKeyPath;
        private String issuer;
        private String audience;
        private Duration accessTokenTtl = Duration.ofMinutes(15);
        private Duration refreshTokenTtl = Duration.ofDays(7);

        public Path getPrivateKeyPath() { return privateKeyPath; }
        public void setPrivateKeyPath(Path privateKeyPath) { this.privateKeyPath = privateKeyPath; }
        public Path getPublicKeyPath() { return publicKeyPath; }
        public void setPublicKeyPath(Path publicKeyPath) { this.publicKeyPath = publicKeyPath; }
        public String getIssuer() { return issuer; }
        public void setIssuer(String issuer) { this.issuer = issuer; }
        public String getAudience() { return audience; }
        public void setAudience(String audience) { this.audience = audience; }
        public Duration getAccessTokenTtl() { return accessTokenTtl; }
        public void setAccessTokenTtl(Duration accessTokenTtl) { this.accessTokenTtl = accessTokenTtl; }
        public Duration getRefreshTokenTtl() { return refreshTokenTtl; }
        public void setRefreshTokenTtl(Duration refreshTokenTtl) { this.refreshTokenTtl = refreshTokenTtl; }
    }

    public static class Cookie {
        private boolean secure = true;
        private String sameSite = "Lax";
        private String domain = "";

        public boolean isSecure() { return secure; }
        public void setSecure(boolean secure) { this.secure = secure; }
        public String getSameSite() { return sameSite; }
        public void setSameSite(String sameSite) { this.sameSite = sameSite; }
        public String getDomain() { return domain; }
        public void setDomain(String domain) { this.domain = domain; }
    }
}
