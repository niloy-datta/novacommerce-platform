package com.novacommerce.auth.api.dto;

import com.novacommerce.auth.domain.user.UserAccount;
import java.util.List;
import java.util.UUID;

public record UserResponse(UUID id, String email, List<String> roles) {
    public static UserResponse from(UserAccount user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getRoles().stream().map(Enum::name).sorted().toList());
    }
}
