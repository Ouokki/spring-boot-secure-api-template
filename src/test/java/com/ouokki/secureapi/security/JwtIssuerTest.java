package com.ouokki.secureapi.security;

import static org.assertj.core.api.Assertions.assertThat;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JwtIssuerTest {

  @TempDir static Path tempDir;

  static JwtIssuer issuer;

  @BeforeAll
  static void setUp() throws NoSuchAlgorithmException, IOException {
    KeyPair keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();

    Path privateKeyPath = tempDir.resolve("private.pem");
    Path publicKeyPath = tempDir.resolve("public.pem");

    Files.writeString(
        privateKeyPath,
        "-----BEGIN PRIVATE KEY-----\n"
            + Base64.getMimeEncoder(64, new byte[] {'\n'})
                .encodeToString(keyPair.getPrivate().getEncoded())
            + "\n-----END PRIVATE KEY-----\n");

    Files.writeString(
        publicKeyPath,
        "-----BEGIN PUBLIC KEY-----\n"
            + Base64.getMimeEncoder(64, new byte[] {'\n'})
                .encodeToString(keyPair.getPublic().getEncoded())
            + "\n-----END PUBLIC KEY-----\n");

    JwtProperties props =
        new JwtProperties(privateKeyPath.toString(), publicKeyPath.toString(), 900L);
    issuer = new JwtIssuer(props);
  }

  @Test
  void issuedTokenContainsExpectedClaims() {
    String token = issuer.issue("user-uuid-123", List.of("ROLE_USER"));
    Claims claims =
        Jwts.parser()
            .verifyWith(issuer.getPublicKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();

    assertThat(claims.getSubject()).isEqualTo("user-uuid-123");
    assertThat(claims.get("roles", List.class)).containsExactly("ROLE_USER");
    assertThat(claims.getId()).isNotBlank();
    assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
  }

  @Test
  void tokenIsSignedWithRs256() {
    String token = issuer.issue("subject", List.of());
    // Parsing with the public key proves the signature — if tampered the parse throws.
    Claims claims =
        Jwts.parser()
            .verifyWith(issuer.getPublicKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    assertThat(claims.getSubject()).isEqualTo("subject");
  }

  @Test
  void eachTokenHasUniqueJti() {
    String t1 = issuer.issue("s", List.of());
    String t2 = issuer.issue("s", List.of());
    Claims c1 =
        Jwts.parser().verifyWith(issuer.getPublicKey()).build().parseSignedClaims(t1).getPayload();
    Claims c2 =
        Jwts.parser().verifyWith(issuer.getPublicKey()).build().parseSignedClaims(t2).getPayload();
    assertThat(c1.getId()).isNotEqualTo(c2.getId());
  }
}
