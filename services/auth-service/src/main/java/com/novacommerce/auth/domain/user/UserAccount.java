package com.novacommerce.auth.domain.user;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users")
public class UserAccount {
    @Id
    private UUID id;
    @Column(nullable = false, length = 320)
    private String email;
    @Column(name = "email_normalized", nullable = false, unique = true, length = 320)
    private String emailNormalized;
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private UserStatus status;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role_name", nullable = false, length = 32)
    private Set<UserRole> roles = EnumSet.noneOf(UserRole.class);
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserAccount() { }

    public UserAccount(UUID id, String email, String emailNormalized, String passwordHash) {
        this.id = id;
        this.email = email;
        this.emailNormalized = emailNormalized;
        this.passwordHash = passwordHash;
        this.status = UserStatus.ACTIVE;
        this.roles.add(UserRole.CUSTOMER);
    }

    @PrePersist
    void created() { createdAt = updatedAt = Instant.now(); }
    @PreUpdate
    void updated() { updatedAt = Instant.now(); }
    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getEmailNormalized() { return emailNormalized; }
    public String getPasswordHash() { return passwordHash; }
    public UserStatus getStatus() { return status; }
    public Set<UserRole> getRoles() { return Set.copyOf(roles); }
    public void addRole(UserRole role) { roles.add(role); }
}
