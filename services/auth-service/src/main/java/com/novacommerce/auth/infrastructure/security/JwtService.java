package com.novacommerce.auth.infrastructure.security;

import com.novacommerce.auth.config.AuthProperties;
import com.novacommerce.auth.domain.user.UserAccount;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private final JwtEncoder encoder;
    private final AuthProperties properties;

    public JwtService(JwtEncoder encoder, AuthProperties properties) { this.encoder = encoder; this.properties = properties; }

    public String issue(UserAccount user) {
        Instant issuedAt = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer(properties.getJwt().getIssuer())
            .audience(java.util.List.of(properties.getJwt().getAudience()))
            .subject(user.getId().toString())
            .issuedAt(issuedAt)
            .notBefore(issuedAt)
            .expiresAt(issuedAt.plus(properties.getJwt().getAccessTokenTtl()))
            .id(UUID.randomUUID().toString())
            .claim("roles", user.getRoles().stream().map(Enum::name).sorted().toList())
            .build();
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).type("JWT").build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
