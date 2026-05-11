package com.ouokki.secureapi.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "users", schema = "app")
public class User {

  @Id
  @Column(nullable = false, updatable = false)
  private UUID id;

  // Stored lower-cased; the DB index is also on lower(email).
  // Normalising in Java (not the DB) keeps the invariant explicit.
  @Column(nullable = false, length = 254)
  private String email;

  @Column(name = "password_hash", nullable = false)
  private String passwordHash;

  @Column(nullable = false)
  private boolean enabled;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  /** JPA requires a no-arg constructor; package-private prevents accidental use. */
  protected User() {}

  private User(UUID id, String email, String passwordHash) {
    this.id = id;
    this.email = email.toLowerCase(Locale.ROOT);
    this.passwordHash = passwordHash;
    this.enabled = true;
  }

  public static User create(String email, String passwordHash) {
    return new User(UUID.randomUUID(), email, passwordHash);
  }

  @PrePersist
  protected void onCreate() {
    Instant now = Instant.now();
    this.createdAt = now;
    this.updatedAt = now;
  }

  @PreUpdate
  protected void onUpdate() {
    this.updatedAt = Instant.now();
  }

  // ─── Equality based on persistent identity, not object identity ────────────

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof User other)) return false;
    return Objects.equals(id, other.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return "User{id=" + id + ", email='" + email + "', enabled=" + enabled + "}";
  }

  // ─── Getters ───────────────────────────────────────────────────────────────

  public UUID getId() {
    return id;
  }

  public String getEmail() {
    return email;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  // ─── Setters for mutable state only ────────────────────────────────────────

  public void setPasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }
}
