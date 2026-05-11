package com.ouokki.secureapi.security;

import io.jsonwebtoken.Jwts;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Issues RS256-signed JWT access tokens.
 *
 * <p>RS256 (RSA + SHA-256) is used instead of HS256 so that the public key can be shared with
 * resource servers for token verification without exposing the signing secret. See ADR 002.
 */
@Service
public class JwtIssuer {

  private final RSAPrivateKey privateKey;
  private final RSAPublicKey publicKey;
  private final long accessTokenTtlSeconds;

  public JwtIssuer(JwtProperties props) {
    this.accessTokenTtlSeconds = props.accessTokenTtlSeconds();
    this.privateKey = loadPrivateKey(props.privateKeyPath());
    this.publicKey = loadPublicKey(props.publicKeyPath());
  }

  public String issue(String subject, List<String> roles) {
    Instant now = Instant.now();
    Instant expiry = now.plusSeconds(accessTokenTtlSeconds);

    return Jwts.builder()
        .subject(subject)
        .issuedAt(Date.from(now))
        .expiration(Date.from(expiry))
        .id(UUID.randomUUID().toString()) // jti — uniqueness for revocation checks
        .claim("roles", roles)
        .signWith(privateKey)
        .compact();
  }

  public RSAPublicKey getPublicKey() {
    return publicKey;
  }

  private static RSAPrivateKey loadPrivateKey(String path) {
    try {
      String pem = readPem(path);
      byte[] der = Base64.getDecoder().decode(pem);
      return (RSAPrivateKey)
          KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
    } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
      throw new IllegalStateException("Failed to load JWT private key from: " + path, e);
    }
  }

  private static RSAPublicKey loadPublicKey(String path) {
    try {
      String pem = readPem(path);
      byte[] der = Base64.getDecoder().decode(pem);
      return (RSAPublicKey)
          KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
    } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
      throw new IllegalStateException("Failed to load JWT public key from: " + path, e);
    }
  }

  /** Strips PEM header/footer lines and whitespace, returning raw Base64. */
  private static String readPem(String path) throws IOException {
    return Files.readString(Path.of(path))
        .replaceAll("-----[A-Z ]+-----", "")
        .replaceAll("\\s", "");
  }
}
