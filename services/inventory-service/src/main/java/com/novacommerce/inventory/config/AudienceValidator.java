package com.novacommerce.inventory.config;
import org.springframework.security.oauth2.core.*; import org.springframework.security.oauth2.jwt.Jwt;
public class AudienceValidator implements OAuth2TokenValidator<Jwt>{private final String audience; public AudienceValidator(String v){audience=v;} public OAuth2TokenValidatorResult validate(Jwt jwt){return jwt.getAudience().contains(audience)?OAuth2TokenValidatorResult.success():OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token","Required audience is missing",null));}}
