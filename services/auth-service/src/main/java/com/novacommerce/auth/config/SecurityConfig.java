package com.novacommerce.auth.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.novacommerce.auth.infrastructure.security.CookieBearerTokenResolver;
import com.novacommerce.auth.infrastructure.security.PemKeyLoader;
import com.novacommerce.auth.infrastructure.security.RsaKeyMaterial;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.KeyPairGenerator;
import java.time.Instant;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.http.HttpStatus;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(AuthProperties.class)
public class SecurityConfig {
    @Bean
    RsaKeyMaterial rsaKeyMaterial(AuthProperties properties, org.springframework.context.ApplicationContext context) {
        if (List.of(context.getEnvironment().getActiveProfiles()).contains("test")) {
            try { var pair = KeyPairGenerator.getInstance("RSA"); pair.initialize(2048); var keyPair = pair.generateKeyPair(); return new RsaKeyMaterial(keyPair.getPublic(), keyPair.getPrivate()); }
            catch (Exception exception) { throw new IllegalStateException("Unable to generate test RSA key pair", exception); }
        }
        return context.getBean(PemKeyLoader.class).load(properties);
    }

    @Bean
    JwtEncoder jwtEncoder(RsaKeyMaterial material) {
        RSAKey key = new RSAKey.Builder((java.security.interfaces.RSAPublicKey) material.publicKey())
            .privateKey((java.security.interfaces.RSAPrivateKey) material.privateKey()).keyID("novacommerce-auth-key").build();
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(key)));
    }

    @Bean
    JwtDecoder jwtDecoder(RsaKeyMaterial material, AuthProperties properties) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey((java.security.interfaces.RSAPublicKey) material.publicKey()).build();
        OAuth2TokenValidator<Jwt> audience = jwt -> jwt.getAudience().contains(properties.getJwt().getAudience())
            ? OAuth2TokenValidatorResult.success()
            : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Invalid token audience", null));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(properties.getJwt().getIssuer()), audience));
        return decoder;
    }

    @Bean
    PasswordEncoder passwordEncoder() { return new Argon2PasswordEncoder(16, 32, 1, 19 * 1024, 2); }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, CookieBearerTokenResolver tokenResolver,
                                            JwtDecoder decoder,
                                            @Qualifier("corsConfigurationSource") CorsConfigurationSource cors) throws Exception {
        CookieCsrfTokenRepository csrf = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrf.setCookieCustomizer(builder -> builder.sameSite("Lax").path("/"));
        http.csrf(config -> config.csrfTokenRepository(csrf))
            .cors(corsConfig -> corsConfig.configurationSource(cors))
            .sessionManagement(config -> config.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/api/v1/auth/csrf", "/api/v1/auth/register", "/api/v1/auth/login", "/api/v1/auth/refresh").permitAll()
                .requestMatchers("/api/v1/auth/me", "/api/v1/auth/logout").authenticated()
                .anyRequest().denyAll())
            .oauth2ResourceServer(oauth -> oauth.bearerTokenResolver(tokenResolver).jwt(jwt -> jwt.decoder(decoder).jwtAuthenticationConverter(jwtAuthenticationConverter())))
            .exceptionHandling(errors -> errors
                .authenticationEntryPoint((request, response, exception) ->
                    writeError(response, HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED", "Authentication required", request.getRequestURI()))
                .accessDeniedHandler(accessDeniedHandler()));
        return http.build();
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName("roles"); roles.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter(); converter.setJwtGrantedAuthoritiesConverter(roles); return converter;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(AuthProperties properties) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(properties.getAllowedOrigins());
        config.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
        config.setAllowedHeaders(List.of("Content-Type", "X-XSRF-TOKEN"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource(); source.registerCorsConfiguration("/api/**", config); return source;
    }

    private AccessDeniedHandler accessDeniedHandler() {
        return (request, response, exception) -> writeError(response, HttpStatus.FORBIDDEN, "INVALID_CSRF_TOKEN", "Access denied", request.getRequestURI());
    }
    private void writeError(HttpServletResponse response, HttpStatus status, String code, String message, String path) throws IOException {
        response.setStatus(status.value()); response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"timestamp\":\"" + Instant.now() + "\",\"status\":" + status.value()
            + ",\"code\":\"" + code + "\",\"message\":\"" + message + "\",\"path\":\"" + path + "\",\"fieldErrors\":{}}");
    }
}
