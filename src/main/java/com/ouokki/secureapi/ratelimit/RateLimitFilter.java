package com.ouokki.secureapi.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(10)
public class RateLimitFilter extends OncePerRequestFilter {

  // NOTE: in-memory buckets — suitable for a single-node deployment.
  // Replace with a distributed cache (Redis + bucket4j-redis) for multi-node.
  private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();
  private final RateLimitProperties props;

  public RateLimitFilter(RateLimitProperties props) {
    this.props = props;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {

    String clientIp = resolveClientIp(request);
    Bucket bucket = buckets.computeIfAbsent(clientIp, ip -> newBucket());

    ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
    response.setHeader("X-RateLimit-Limit", String.valueOf(props.requestsPerMinute()));
    response.setHeader(
        "X-RateLimit-Remaining", String.valueOf(Math.max(0, probe.getRemainingTokens())));

    if (probe.isConsumed()) {
      chain.doFilter(request, response);
    } else {
      long retryAfter = TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill());
      response.setHeader("Retry-After", String.valueOf(retryAfter));
      response.setStatus(429);
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
      response.getWriter().write("{\"status\":429,\"error\":\"Too Many Requests\"}");
    }
  }

  private Bucket newBucket() {
    Bandwidth limit =
        Bandwidth.builder()
            .capacity(props.requestsPerMinute())
            .refillGreedy(props.requestsPerMinute(), Duration.ofMinutes(1))
            .initialTokens(Math.min(props.burstCapacity(), props.requestsPerMinute()))
            .build();
    return Bucket.builder().addLimit(limit).build();
  }

  private String resolveClientIp(HttpServletRequest request) {
    String forwarded = request.getHeader("X-Forwarded-For");
    if (forwarded != null && !forwarded.isBlank()) {
      return forwarded.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }
}
