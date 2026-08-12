package com.novacommerce.catalog.integration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.novacommerce.catalog.infrastructure.persistence.BrandRepository;
import com.sun.net.httpserver.HttpServer;
import jakarta.servlet.http.Cookie;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CatalogJwtJwksIntegrationTests {
    private static final String KEY_ID = "novacommerce-auth-key";
    private static final RSAKey SIGNING_KEY = generateKey(KEY_ID);
    private static final RSAKey WRONG_KEY = generateKey(KEY_ID);
    private static final HttpServer JWKS_SERVER = startJwksServer();

    @DynamicPropertySource
    static void jwksProperties(DynamicPropertyRegistry registry) {
        registry.add("catalog.auth.jwks-uri", () -> "http://127.0.0.1:" + JWKS_SERVER.getAddress().getPort() + "/jwks");
    }

    @Autowired MockMvc mvc;
    @Autowired BrandRepository brands;

    @BeforeEach
    void clearDatabase() { brands.deleteAll(); }

    @AfterAll
    static void stopServer() { JWKS_SERVER.stop(0); }

    @Test
    void verifiesRealAdminJwtThroughPublishedJwksAndEnforcesCsrf() throws Exception {
        Cookie admin = accessCookie(token(SIGNING_KEY, "test-issuer", "test-audience", "ADMIN"));
        Cookie customer = accessCookie(token(SIGNING_KEY, "test-issuer", "test-audience", "CUSTOMER"));
        String body = "{\"name\":\"JWKS Brand\",\"slug\":\"jwks-brand\"}";

        mvc.perform(post("/api/v1/admin/catalog/brands").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/admin/catalog/brands").cookie(customer).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/admin/catalog/brands").cookie(admin).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/admin/catalog/brands").cookie(admin).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.slug").value("jwks-brand"));
    }

    @Test
    void rejectsWrongIssuerAudienceAndSignature() throws Exception {
        assertRejected(token(SIGNING_KEY, "wrong-issuer", "test-audience", "ADMIN"));
        assertRejected(token(SIGNING_KEY, "test-issuer", "wrong-audience", "ADMIN"));
        assertRejected(token(WRONG_KEY, "test-issuer", "test-audience", "ADMIN"));
    }

    private void assertRejected(String token) throws Exception {
        mvc.perform(post("/api/v1/admin/catalog/brands").cookie(accessCookie(token)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Rejected\",\"slug\":\"rejected\"}"))
            .andExpect(status().isUnauthorized());
    }

    private static Cookie accessCookie(String token) { return new Cookie("NC_ACCESS", token); }

    private static String token(RSAKey key, String issuer, String audience, String role) throws Exception {
        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
            .issuer(issuer).audience(audience).subject(UUID.randomUUID().toString())
            .issueTime(Date.from(now)).notBeforeTime(Date.from(now.minusSeconds(1)))
            .expirationTime(Date.from(now.plusSeconds(300))).jwtID(UUID.randomUUID().toString())
            .claim("roles", List.of(role)).build();
        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).type(JOSEObjectType.JWT).keyID(KEY_ID).build(), claims);
        jwt.sign(new RSASSASigner(key.toRSAPrivateKey()));
        return jwt.serialize();
    }

    private static RSAKey generateKey(String keyId) {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            var pair = generator.generateKeyPair();
            return new RSAKey.Builder((RSAPublicKey) pair.getPublic()).privateKey((RSAPrivateKey) pair.getPrivate()).keyID(keyId).build();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to generate test RSA key", exception);
        }
    }

    private static HttpServer startJwksServer() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/jwks", exchange -> {
                byte[] body = new JWKSet(SIGNING_KEY.toPublicJWK()).toString().getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
                exchange.close();
            });
            server.start();
            return server;
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to start test JWKS server", exception);
        }
    }
}
