package com.novacommerce.payment.config;

import com.novacommerce.payment.infrastructure.security.CookieBearerTokenResolver;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableConfigurationProperties(PaymentProperties.class)
public class SecurityConfig {

    @Bean
    JwtDecoder paymentJwtDecoder(PaymentProperties p) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(p.getAuth().getJwksUri()).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(p.getAuth().getIssuer()),
                new AudienceValidator(p.getAuth().getAudience())
        ));
        return decoder;
    }

    @Bean
    SecurityFilterChain paymentSecurityFilterChain(HttpSecurity http, CookieBearerTokenResolver resolver,
                                                    JwtDecoder decoder, @Qualifier("paymentCors") CorsConfigurationSource cors) throws Exception {
        JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName("roles");
        roles.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter convert = new JwtAuthenticationConverter();
        convert.setJwtGrantedAuthoritiesConverter(roles);

        JwtAuthenticationProvider provider = new JwtAuthenticationProvider(decoder);
        provider.setJwtAuthenticationConverter(convert);

        BearerTokenAuthenticationFilter filter = new BearerTokenAuthenticationFilter(
                new ProviderManager(provider),
                request -> {
                    String token = resolver.resolve(request);
                    return token == null ? null : new BearerTokenAuthenticationToken(token);
                }
        );

        CookieCsrfTokenRepository csrf = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrf.setCookieCustomizer(b -> b.sameSite("Lax").path("/"));

        http.csrf(c -> c.csrfTokenRepository(csrf).ignoringRequestMatchers("/api/v1/payments/webhooks/**"))
                .cors(c -> c.configurationSource(cors))
                .sessionManagement(c -> c.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/actuator/health", "/api/v1/payments/csrf", "/api/v1/payments/webhooks/**").permitAll()
                        .requestMatchers("/api/v1/payments/**").hasAnyRole("CUSTOMER", "ADMIN")
                        .anyRequest().denyAll()
                )
                .addFilterBefore(filter, AnonymousAuthenticationFilter.class)
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, resp, authEx) -> resp.sendError(401))
                        .accessDeniedHandler((req, resp, accessEx) -> resp.sendError(403))
                );

        return http.build();
    }

    @Bean(name = "paymentCors")
    CorsConfigurationSource paymentCorsConfigurationSource(PaymentProperties p) {
        CorsConfiguration c = new CorsConfiguration();
        c.setAllowedOrigins(p.getAllowedOrigins());
        c.setAllowedMethods(List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS"));
        c.setAllowedHeaders(List.of("Content-Type", "X-XSRF-TOKEN", "Idempotency-Key"));
        c.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource s = new UrlBasedCorsConfigurationSource();
        s.registerCorsConfiguration("/api/**", c);
        return s;
    }
}
