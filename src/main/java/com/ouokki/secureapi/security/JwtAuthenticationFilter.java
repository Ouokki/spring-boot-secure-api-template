package com.ouokki.secureapi.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
  private static final String BEARER_PREFIX = "Bearer ";

  private final JwtIssuer jwtIssuer;

  public JwtAuthenticationFilter(JwtIssuer jwtIssuer) {
    this.jwtIssuer = jwtIssuer;
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {

    String token = extractToken(request);
    if (token == null) {
      filterChain.doFilter(request, response);
      return;
    }

    try {
      Claims claims =
          Jwts.parser()
              .verifyWith(jwtIssuer.getPublicKey())
              .build()
              .parseSignedClaims(token)
              .getPayload();

      List<?> rawRoles = claims.get("roles", List.class);
      List<SimpleGrantedAuthority> authorities =
          rawRoles == null
              ? List.of()
              : rawRoles.stream().map(r -> new SimpleGrantedAuthority(r.toString())).toList();

      UsernamePasswordAuthenticationToken auth =
          new UsernamePasswordAuthenticationToken(claims.getSubject(), null, authorities);
      auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
      SecurityContextHolder.getContext().setAuthentication(auth);

    } catch (ExpiredJwtException e) {
      log.debug("Rejected expired JWT: {}", e.getMessage());
      SecurityContextHolder.clearContext();
    } catch (JwtException e) {
      log.debug("Rejected invalid JWT: {}", e.getMessage());
      SecurityContextHolder.clearContext();
    }

    filterChain.doFilter(request, response);
  }

  private static String extractToken(HttpServletRequest request) {
    String header = request.getHeader("Authorization");
    if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
      return header.substring(BEARER_PREFIX.length());
    }
    return null;
  }
}
