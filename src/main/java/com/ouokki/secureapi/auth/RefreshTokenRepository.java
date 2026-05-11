package com.ouokki.secureapi.auth;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

  Optional<RefreshToken> findByTokenHash(String tokenHash);

  List<RefreshToken> findByFamilyId(UUID familyId);

  /** Revokes every non-revoked token in a family — used on suspected token theft. */
  @Modifying
  @Query(
      "UPDATE RefreshToken t SET t.revokedAt = CURRENT_TIMESTAMP "
          + "WHERE t.familyId = :familyId AND t.revokedAt IS NULL")
  void revokeFamily(@Param("familyId") UUID familyId);
}
