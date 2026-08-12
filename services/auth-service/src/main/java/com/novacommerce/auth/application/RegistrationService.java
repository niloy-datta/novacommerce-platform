package com.novacommerce.auth.application;

import com.novacommerce.auth.api.dto.RegisterRequest;
import com.novacommerce.auth.api.error.AuthException;
import com.novacommerce.auth.domain.user.UserAccount;
import com.novacommerce.auth.infrastructure.persistence.UserAccountRepository;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationService {
    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;
    public RegistrationService(UserAccountRepository users, PasswordEncoder passwordEncoder) { this.users = users; this.passwordEncoder = passwordEncoder; }

    @Transactional
    public UserAccount register(RegisterRequest request) {
        String normalized = normalize(request.email());
        if (users.existsByEmailNormalized(normalized)) {
            throw new AuthException(HttpStatus.CONFLICT, "EMAIL_ALREADY_REGISTERED", "Email is already registered");
        }
        UserAccount user = new UserAccount(UUID.randomUUID(), normalized, normalized, passwordEncoder.encode(request.password()));
        return users.save(user);
    }

    public String normalize(String email) { return email.trim().toLowerCase(Locale.ROOT); }
}
