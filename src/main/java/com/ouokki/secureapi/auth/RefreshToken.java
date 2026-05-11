package com.ouokki.secureapi.auth;

import com.ouokki.secureapi.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens", schema = "app")
public class RefreshToken {

  @Id
  @Column(nullable = false, updatable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false, updatable = false)
  private User user;

  @Column(name = "token_hash", nullable = false, unique = true, updatable = false)
  private String tokenHash;

  @Column(name = "family_id", nullable = false, updatable = false)
  private UUID familyId;

  @Column(name = "replaced_by")
  private UUID replacedBy;

  @Column(name = "revoked_at")
  private Instant revokedAt;

  @Column(name = "expires_at", nullable = false, updatable = false)
  private Instant expiresAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected RefreshToken() {}

  private RefreshToken(UUID id, User user, String tokenHash, UUID familyId, Instant expiresAt) {
    this.id = id;
    this.user = user;
    this.tokenHash = tokenHash;
    this.familyId = familyId;
    this.expiresAt = expiresAt;
  }

  public static RefreshToken create(User user, String tokenHash, UUID familyId, Instant expiresAt) {
    return new RefreshToken(UUID.randomUUID(), user, tokenHash, familyId, expiresAt);
  }

  @PrePersist
  protected void onCreate() {
    this.createdAt = Instant.now();
  }

  public boolean isRevoked() {
    return revokedAt != null;
  }

  public boolean isExpired() {
    return Instant.now().isAfter(expiresAt);
  }

  public void revoke() {
    this.revokedAt = Instant.now();
  }

  public void markReplacedBy(UUID newTokenId) {
    this.replacedBy = newTokenId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof RefreshToken other)) return false;
    return Objects.equals(id, other.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  public UUID getId() {
    return id;
  }

  public User getUser() {
    return user;
  }

  public String getTokenHash() {
    return tokenHash;
  }

  public UUID getFamilyId() {
    return familyId;
  }

  public UUID getReplacedBy() {
    return replacedBy;
  }

  public Instant getRevokedAt() {
    return revokedAt;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }
}
