package com.novacommerce.order.integration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import jakarta.servlet.http.Cookie;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
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
class OrderFastIntegrationTests {
    private static final String KEY_ID = "order-test-key";
    private static final RSAKey KEY = createKey();
    private static final HttpServer JWKS_SERVER = startJwksServer();

    @Autowired MockMvc mvc;
    UUID owner;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("order.auth.jwks-uri",
                () -> "http://127.0.0.1:" + JWKS_SERVER.getAddress().getPort() + "/jwks");
    }

    @AfterAll
    static void stopServer() {
        JWKS_SERVER.stop(0);
    }

    @BeforeEach
    void setup() {
        owner = UUID.randomUUID();
    }

    @Test
    void realCookieJwtRequiresCsrfForCartMutation() throws Exception {
        mvc.perform(get("/api/v1/cart")).andExpect(status().isUnauthorized());

        Cookie access = new Cookie("NC_ACCESS", token(owner, "CUSTOMER"));
        mvc.perform(get("/api/v1/cart").cookie(access))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACTIVE"));

        String body = "{\"variantId\":\"" + UUID.randomUUID() + "\",\"quantity\":2}";
        mvc.perform(post("/api/v1/cart/items").cookie(access)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/cart/items").cookie(access).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].quantity").value(2));
    }

    @Test
    void invalidQuantityIsRejected() throws Exception {
        mvc.perform(post("/api/v1/cart/items")
                        .with(jwt().jwt(jwt -> jwt.subject(owner.toString())).authorities(() -> "ROLE_CUSTOMER"))
                        .with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"variantId\":\"" + UUID.randomUUID() + "\",\"quantity\":0}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    private static String token(UUID subject, String role) throws Exception {
        Instant now = Instant.now();
        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(KEY_ID).build(),
                new JWTClaimsSet.Builder().issuer("test-issuer").audience("test-audience")
                        .subject(subject.toString()).issueTime(Date.from(now))
                        .expirationTime(Date.from(now.plusSeconds(300))).claim("roles", List.of(role)).build());
        jwt.sign(new RSASSASigner(KEY.toRSAPrivateKey()));
        return jwt.serialize();
    }

    private static RSAKey createKey() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair pair = generator.generateKeyPair();
            return new RSAKey.Builder((RSAPublicKey) pair.getPublic())
                    .privateKey((RSAPrivateKey) pair.getPrivate()).keyID(KEY_ID).build();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static HttpServer startJwksServer() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/jwks", exchange -> {
                byte[] body = new JWKSet(KEY.toPublicJWK()).toString().getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
                exchange.close();
            });
            server.start();
            return server;
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
