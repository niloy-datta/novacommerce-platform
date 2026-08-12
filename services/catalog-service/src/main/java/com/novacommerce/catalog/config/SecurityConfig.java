package com.novacommerce.catalog.config;

import com.novacommerce.catalog.infrastructure.security.CookieBearerTokenResolver;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(CatalogProperties.class)
public class SecurityConfig {
    @Bean
    JwtDecoder catalogJwtDecoder(CatalogProperties properties) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(properties.getAuth().getJwksUri()).build();
        OAuth2TokenValidator<Jwt> issuer = JwtValidators.createDefaultWithIssuer(properties.getAuth().getIssuer());
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(issuer, new AudienceValidator(properties.getAuth().getAudience())));
        return decoder;
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName("roles");
        roles.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(roles);
        return converter;
    }

    @Bean
    SecurityFilterChain catalogSecurityFilterChain(HttpSecurity http, CookieBearerTokenResolver resolver,
                                                   JwtDecoder decoder, JwtAuthenticationConverter converter,
                                                   @Qualifier("catalogCorsConfigurationSource") CorsConfigurationSource cors) throws Exception {
        CookieCsrfTokenRepository csrf = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrf.setCookieCustomizer(builder -> builder.sameSite("Lax").path("/"));
        RequestMatcher apiMutations = request -> !List.of("GET", "HEAD", "TRACE", "OPTIONS").contains(request.getMethod())
            && request.getRequestURI().startsWith("/api/");
        JwtAuthenticationProvider jwtProvider = new JwtAuthenticationProvider(decoder);
        jwtProvider.setJwtAuthenticationConverter(converter);
        AuthenticationConverter cookieToken = request -> {
            String token = resolver.resolve(request);
            return token == null ? null : new BearerTokenAuthenticationToken(token);
        };
        BearerTokenAuthenticationFilter bearerFilter = new BearerTokenAuthenticationFilter(new ProviderManager(jwtProvider), cookieToken);
        bearerFilter.setAuthenticationEntryPoint((request, response, exception) ->
            writeError(response, HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED", "Authentication required", request.getRequestURI()));
        http.csrf(config -> config.csrfTokenRepository(csrf).requireCsrfProtectionMatcher(apiMutations))
            .cors(config -> config.configurationSource(cors))
            .sessionManagement(config -> config.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/api/v1/catalog/csrf").permitAll()
                .requestMatchers("/api/v1/products/**", "/api/v1/categories/**", "/api/v1/brands/**").permitAll()
                .requestMatchers("/api/v1/admin/catalog/**").hasRole("ADMIN")
                .anyRequest().denyAll())
            .addFilterBefore(bearerFilter, AnonymousAuthenticationFilter.class)
            .exceptionHandling(errors -> errors
                .authenticationEntryPoint((request, response, exception) -> writeError(response, HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED", "Authentication required", request.getRequestURI()))
                .accessDeniedHandler(accessDeniedHandler()));
        return http.build();
    }

    @Bean(name = "catalogCorsConfigurationSource")
    CorsConfigurationSource catalogCorsConfigurationSource(CatalogProperties properties) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(properties.getAllowedOrigins());
        config.setAllowedMethods(List.of("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Content-Type", "X-XSRF-TOKEN"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    private AccessDeniedHandler accessDeniedHandler() {
        return (request, response, exception) -> writeError(response, HttpStatus.FORBIDDEN, "ACCESS_DENIED", "Access denied", request.getRequestURI());
    }

    private void writeError(HttpServletResponse response, HttpStatus status, String code, String message, String path) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"timestamp\":\"" + Instant.now() + "\",\"status\":" + status.value()
            + ",\"code\":\"" + code + "\",\"message\":\"" + message + "\",\"path\":\"" + path + "\"}");
    }
}
