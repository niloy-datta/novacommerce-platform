package com.novacommerce.auth.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.novacommerce.auth.application.AuthenticationService;
import com.novacommerce.auth.application.RefreshTokenService;
import com.novacommerce.auth.domain.token.RefreshToken;
import com.novacommerce.auth.domain.user.UserAccount;
import com.novacommerce.auth.domain.user.UserRole;
import com.novacommerce.auth.infrastructure.persistence.RefreshTokenRepository;
import com.novacommerce.auth.infrastructure.persistence.UserAccountRepository;
import com.novacommerce.auth.infrastructure.security.JwtService;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.KeyPairGenerator;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthServiceIntegrationTests {
    private static final String EMAIL = "User@Example.com";
    private static final String PASSWORD = "correct horse battery";
    @Autowired MockMvc mvc;
    @Autowired UserAccountRepository users;
    @Autowired RefreshTokenRepository refreshTokens;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired AuthenticationService authentication;
    @Autowired RefreshTokenService tokenService;
    @Autowired JwtService jwtService;
    @Autowired JwtEncoder jwtEncoder;
    @Autowired JwtDecoder jwtDecoder;
    @Autowired JwtAuthenticationConverter jwtAuthenticationConverter;

    @BeforeEach
    void clearDatabase() { refreshTokens.deleteAll(); users.deleteAll(); }

    @Test
    void registrationCreatesOnlyNormalizedCustomerWithArgon2Hash() throws Exception {
        mvc.perform(post("/api/v1/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + EMAIL + "\",\"password\":\"" + PASSWORD + "\",\"role\":\"ADMIN\"}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.email").value("user@example.com"))
            .andExpect(jsonPath("$.roles[0]").value("CUSTOMER"));
        UserAccount user = users.findByEmailNormalized("user@example.com").orElseThrow();
        assertThat(user.getPasswordHash()).startsWith("$argon2").doesNotContain(PASSWORD);
        assertThat(passwordEncoder.matches(PASSWORD, user.getPasswordHash())).isTrue();
        assertThat(user.getRoles()).extracting(Enum::name).containsExactly("CUSTOMER");
    }

    @Test
    void registrationRejectsDuplicatesInvalidInputAndMissingCsrf() throws Exception {
        register();
        mvc.perform(post("/api/v1/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"user@example.com\",\"password\":\"" + PASSWORD + "\"}"))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("EMAIL_ALREADY_REGISTERED"));
        mvc.perform(post("/api/v1/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"not-an-email\",\"password\":\"short\"}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"new@example.com\",\"password\":\"" + PASSWORD + "\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void loginUsesHttpOnlyCookiesWithoutTokensInJsonAndMeUsesAccessCookie() throws Exception {
        register();
        MvcResult result = login(PASSWORD);
        List<String> cookies = result.getResponse().getHeaders("Set-Cookie");
        assertThat(cookies).anyMatch(value -> value.startsWith("NC_ACCESS=") && value.contains("HttpOnly") && value.contains("Path=/"));
        assertThat(cookies).anyMatch(value -> value.startsWith("NC_REFRESH=") && value.contains("HttpOnly") && value.contains("Path=/api/v1/auth"));
        assertThat(result.getResponse().getContentAsString()).doesNotContain("token");
        mvc.perform(get("/api/v1/auth/me").cookie(new Cookie("NC_ACCESS", cookie(cookies, "NC_ACCESS"))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.email").value("user@example.com"));
        mvc.perform(post("/api/v1/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"missing@example.com\",\"password\":\"wrong-password\"}"))
            .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        mvc.perform(post("/api/v1/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"user@example.com\",\"password\":\"wrong-password\"}"))
            .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void refreshRotatesAndDetectsReplay() throws Exception {
        register();
        String first = cookie(login(PASSWORD).getResponse().getHeaders("Set-Cookie"), "NC_REFRESH");
        MvcResult rotated = mvc.perform(post("/api/v1/auth/refresh").with(csrf()).cookie(new Cookie("NC_REFRESH", first)))
            .andExpect(status().isNoContent()).andReturn();
        String second = cookie(rotated.getResponse().getHeaders("Set-Cookie"), "NC_REFRESH");
        assertThat(second).isNotEqualTo(first);
        assertThat(refreshTokens.findAll()).allSatisfy(token -> assertThat(token.getTokenHash()).doesNotContain(first));
        mvc.perform(post("/api/v1/auth/refresh").with(csrf()).cookie(new Cookie("NC_REFRESH", first)))
            .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("REFRESH_TOKEN_REUSE_DETECTED"));
        mvc.perform(post("/api/v1/auth/refresh").with(csrf()).cookie(new Cookie("NC_REFRESH", second)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutRequiresCsrfRevokesTokenAndClearsCookies() throws Exception {
        register();
        List<String> loginCookies = login(PASSWORD).getResponse().getHeaders("Set-Cookie");
        String refresh = cookie(loginCookies, "NC_REFRESH");
        String access = cookie(loginCookies, "NC_ACCESS");
        mvc.perform(post("/api/v1/auth/logout").cookie(new Cookie("NC_REFRESH", refresh))).andExpect(status().isForbidden());
        MvcResult logout = mvc.perform(post("/api/v1/auth/logout").with(csrf())
                .cookie(new Cookie("NC_REFRESH", refresh), new Cookie("NC_ACCESS", access)))
            .andExpect(status().isNoContent()).andReturn();
        assertThat(logout.getResponse().getHeaders("Set-Cookie")).anyMatch(value -> value.startsWith("NC_ACCESS=") && value.contains("Max-Age=0"));
        mvc.perform(post("/api/v1/auth/refresh").with(csrf()).cookie(new Cookie("NC_REFRESH", refresh))).andExpect(status().isUnauthorized());
    }

    @Test
    void concurrentRefreshCreatesOnlyOneSuccessfulRotation() throws Exception {
        register();
        String raw = authentication.login(new com.novacommerce.auth.api.dto.LoginRequest(EMAIL, PASSWORD)).refreshToken();
        CountDownLatch start = new CountDownLatch(1);
        Callable<Boolean> rotate = () -> { start.await(); try { tokenService.rotate(raw); return true; } catch (RuntimeException exception) { return false; } };
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> first = executor.submit(rotate); Future<Boolean> second = executor.submit(rotate); start.countDown();
            assertThat(List.of(first.get(), second.get())).containsExactlyInAnyOrder(true, false);
        } finally { executor.shutdownNow(); }
    }

    @Test
    void csrfEndpointAndProtectedResourceEnforceBrowserProtections() throws Exception {
        mvc.perform(get("/api/v1/auth/csrf"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.token").isNotEmpty())
            .andExpect(jsonPath("$.headerName").value("X-XSRF-TOKEN"));
        mvc.perform(post("/api/v1/auth/refresh")).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/auth/me"))
            .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void rejectsExpiredAndInvalidAccessTokensAndMapsCustomerAndAdminAuthorities() throws Exception {
        register();
        UserAccount user = users.findByEmailNormalized("user@example.com").orElseThrow();
        String valid = jwtService.issue(user);
        Jwt customerJwt = jwtDecoder.decode(valid);
        assertThat(jwtAuthenticationConverter.convert(customerJwt).getAuthorities())
            .extracting(Object::toString).contains("ROLE_CUSTOMER");
        user.addRole(UserRole.ADMIN);
        users.saveAndFlush(user);
        Jwt adminJwt = jwtDecoder.decode(jwtService.issue(user));
        assertThat(jwtAuthenticationConverter.convert(adminJwt).getAuthorities())
            .extracting(Object::toString).contains("ROLE_CUSTOMER", "ROLE_ADMIN");

        mvc.perform(get("/api/v1/auth/me").cookie(new Cookie("NC_ACCESS", signedToken("wrong-issuer", "test-audience", Instant.now().plusSeconds(60)))))
            .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/auth/me").cookie(new Cookie("NC_ACCESS", signedToken("test-issuer", "wrong-audience", Instant.now().plusSeconds(60)))))
            .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/auth/me").cookie(new Cookie("NC_ACCESS", signedToken("test-issuer", "test-audience", Instant.now().minusSeconds(60)))))
            .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/auth/me").cookie(new Cookie("NC_ACCESS", tokenSignedByAnotherKey())))
            .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/auth/me").cookie(new Cookie("NC_ACCESS", "not-a-jwt"))).andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsUnknownAndExpiredRefreshTokens() throws Exception {
        mvc.perform(post("/api/v1/auth/refresh").with(csrf()).cookie(new Cookie("NC_REFRESH", "unknown")))
            .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
        register();
        UserAccount user = users.findByEmailNormalized("user@example.com").orElseThrow();
        String raw = "expired-refresh-token";
        refreshTokens.saveAndFlush(new RefreshToken(user, sha256(raw), java.util.UUID.randomUUID(), Instant.now().minusSeconds(1)));
        mvc.perform(post("/api/v1/auth/refresh").with(csrf()).cookie(new Cookie("NC_REFRESH", raw)))
            .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    private void register() throws Exception {
        mvc.perform(post("/api/v1/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"" + EMAIL + "\",\"password\":\"" + PASSWORD + "\"}")).andExpect(status().isCreated());
    }
    private MvcResult login(String password) throws Exception {
        return mvc.perform(post("/api/v1/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"" + EMAIL + "\",\"password\":\"" + password + "\"}")).andExpect(status().isOk()).andReturn();
    }
    private String cookie(List<String> values, String name) {
        return values.stream().filter(value -> value.startsWith(name + "=")).findFirst().orElseThrow().split(";", 2)[0].substring(name.length() + 1);
    }
    private String signedToken(String issuer, String audience, Instant expiresAt) {
        Instant issuedAt = Instant.now().minusSeconds(120);
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer(issuer).audience(List.of(audience)).subject(java.util.UUID.randomUUID().toString())
            .issuedAt(issuedAt).notBefore(issuedAt).expiresAt(expiresAt).id(java.util.UUID.randomUUID().toString()).claim("roles", List.of("CUSTOMER")).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256).type("JWT").build(), claims)).getTokenValue();
    }
    private String sha256(String raw) throws Exception {
        return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)));
    }
    private String tokenSignedByAnotherKey() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        var pair = generator.generateKeyPair();
        RSAKey key = new RSAKey.Builder((java.security.interfaces.RSAPublicKey) pair.getPublic())
            .privateKey((java.security.interfaces.RSAPrivateKey) pair.getPrivate()).build();
        JwtEncoder otherEncoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(key)));
        Instant issuedAt = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer("test-issuer").audience(List.of("test-audience"))
            .subject(java.util.UUID.randomUUID().toString()).issuedAt(issuedAt).notBefore(issuedAt).expiresAt(issuedAt.plusSeconds(60))
            .id(java.util.UUID.randomUUID().toString()).claim("roles", List.of("CUSTOMER")).build();
        return otherEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256).type("JWT").build(), claims)).getTokenValue();
    }
}
