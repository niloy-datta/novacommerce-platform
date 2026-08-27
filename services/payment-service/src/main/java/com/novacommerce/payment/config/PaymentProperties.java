package com.novacommerce.payment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.List;

@ConfigurationProperties(prefix = "payment")
public class PaymentProperties {

    private Auth auth = new Auth();
    private List<String> allowedOrigins = List.of("http://localhost:3000");
    private String provider = "MOCK";
    private Stripe stripe = new Stripe();
    private long webhookToleranceSeconds = 300;

    public Auth getAuth() {
        return auth;
    }

    public void setAuth(Auth auth) {
        this.auth = auth;
    }

    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public long getWebhookToleranceSeconds() { return webhookToleranceSeconds; }
    public void setWebhookToleranceSeconds(long value) { webhookToleranceSeconds = value; }

    public Stripe getStripe() {
        return stripe;
    }

    public void setStripe(Stripe stripe) {
        this.stripe = stripe;
    }

    public static class Auth {
        private String issuer = "novacommerce-auth";
        private String audience = "novacommerce-api";
        private String jwksUri = "http://localhost:8081/api/v1/auth/jwks";

        public String getIssuer() {
            return issuer;
        }

        public void setIssuer(String issuer) {
            this.issuer = issuer;
        }

        public String getAudience() {
            return audience;
        }

        public void setAudience(String audience) {
            this.audience = audience;
        }

        public String getJwksUri() {
            return jwksUri;
        }

        public void setJwksUri(String jwksUri) {
            this.jwksUri = jwksUri;
        }
    }

    public static class Stripe {
        private String secretKey = "";
        private String webhookSecret = "";

        public String getSecretKey() {
            return secretKey;
        }

        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }

        public String getWebhookSecret() {
            return webhookSecret;
        }

        public void setWebhookSecret(String webhookSecret) {
            this.webhookSecret = webhookSecret;
        }
    }
}
