package com.ouokki.secureapi.user;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

  // Email is stored lower-cased but we lower-case the param too for safety.
  @Query("SELECT u FROM User u WHERE lower(u.email) = lower(:email)")
  Optional<User> findByEmail(@Param("email") String email);

  boolean existsByEmail(String email);
}
