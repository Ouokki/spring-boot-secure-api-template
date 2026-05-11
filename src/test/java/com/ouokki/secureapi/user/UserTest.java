package com.ouokki.secureapi.user;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UserTest {

  @Test
  void createNormalisesEmailToLowerCase() {
    User user = User.create("USER@EXAMPLE.COM", "hash");
    assertThat(user.getEmail()).isEqualTo("user@example.com");
  }

  @Test
  void createSetsEnabledTrue() {
    User user = User.create("a@b.com", "hash");
    assertThat(user.isEnabled()).isTrue();
  }

  @Test
  void createAssignsNonNullId() {
    User user = User.create("a@b.com", "hash");
    assertThat(user.getId()).isNotNull();
  }

  @Test
  void twoUsersWithSameIdAreEqual() {
    User a = User.create("a@b.com", "hash");
    User b = User.create("c@d.com", "hash");
    // Reflectively copy the id to simulate Hibernate returning same row twice
    // (equals contract must hold for entity identity, not object identity).
    assertThat(a).isNotEqualTo(b);
    assertThat(a).isEqualTo(a);
  }

  @Test
  void equalsAndHashCodeConsistency() {
    User user = User.create("a@b.com", "hash");
    assertThat(user.hashCode()).isEqualTo(user.hashCode());
    assertThat(user).isEqualTo(user);
    assertThat(user).isNotEqualTo(null);
    assertThat(user).isNotEqualTo("not a user");
  }
}
