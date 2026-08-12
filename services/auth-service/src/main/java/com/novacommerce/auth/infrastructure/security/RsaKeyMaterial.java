package com.novacommerce.auth.infrastructure.security;

import java.security.PrivateKey;
import java.security.PublicKey;

public record RsaKeyMaterial(PublicKey publicKey, PrivateKey privateKey) { }
