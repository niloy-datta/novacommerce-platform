package com.novacommerce.auth.infrastructure.security;

import com.novacommerce.auth.config.AuthProperties;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class PemKeyLoader {
    public RsaKeyMaterial load(AuthProperties properties) {
        try {
            if (properties.getJwt().getPrivateKeyPath() == null || properties.getJwt().getPublicKeyPath() == null) {
                throw new IllegalStateException("AUTH_JWT_PRIVATE_KEY_PATH and AUTH_JWT_PUBLIC_KEY_PATH are required outside the test profile");
            }
            KeyFactory factory = KeyFactory.getInstance("RSA");
            PrivateKey privateKey = factory.generatePrivate(new PKCS8EncodedKeySpec(readPem(properties.getJwt().getPrivateKeyPath().toString())));
            PublicKey publicKey = factory.generatePublic(new X509EncodedKeySpec(readPem(properties.getJwt().getPublicKeyPath().toString())));
            return new RsaKeyMaterial(publicKey, privateKey);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to load configured RSA JWT keys", exception);
        }
    }

    private byte[] readPem(String location) throws Exception {
        String pem = Files.readString(java.nio.file.Path.of(location));
        String body = pem.replaceAll("-----BEGIN [A-Z ]+-----", "").replaceAll("-----END [A-Z ]+-----", "").replaceAll("\\s", "");
        return Base64.getDecoder().decode(body);
    }
}
