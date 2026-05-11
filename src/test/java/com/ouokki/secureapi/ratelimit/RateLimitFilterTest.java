package com.ouokki.secureapi.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RateLimitFilterTest {

  // Limit=3, burst=2: bucket starts with 2 tokens.
  RateLimitFilter filter;

  @BeforeEach
  void setUp() {
    filter = new RateLimitFilter(new RateLimitProperties(3, 2));
  }

  @Test
  void requestsWithinBurstPassThrough() throws Exception {
    MockHttpServletRequest request = requestFrom("10.0.0.1");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = mock(FilterChain.class);

    filter.doFilter(request, response, chain);

    verify(chain).doFilter(request, response);
    assertThat(response.getStatus()).isNotEqualTo(429);
    assertThat(response.getHeader("X-RateLimit-Limit")).isEqualTo("3");
    assertThat(response.getHeader("X-RateLimit-Remaining")).isEqualTo("1");
  }

  @Test
  void requestsExceedingBurstReturn429() throws Exception {
    MockHttpServletRequest request = requestFrom("10.0.0.2");

    // Exhaust the 2 burst tokens.
    for (int i = 0; i < 2; i++) {
      FilterChain chain = mock(FilterChain.class);
      filter.doFilter(request, new MockHttpServletResponse(), chain);
    }

    // Third request exceeds burst — should be rate-limited.
    MockHttpServletResponse blocked = new MockHttpServletResponse();
    FilterChain chain = mock(FilterChain.class);
    filter.doFilter(request, blocked, chain);

    assertThat(blocked.getStatus()).isEqualTo(429);
    assertThat(blocked.getHeader("Retry-After")).isNotNull();
    assertThat(blocked.getHeader("X-RateLimit-Remaining")).isEqualTo("0");
    verify(chain, never()).doFilter(request, blocked);
  }

  @Test
  void differentIpsHaveIndependentBuckets() throws Exception {
    // Exhaust bucket for IP A.
    MockHttpServletRequest ipA = requestFrom("192.168.1.1");
    for (int i = 0; i < 2; i++) {
      filter.doFilter(ipA, new MockHttpServletResponse(), mock(FilterChain.class));
    }

    // IP B should still pass (independent bucket, starting at burst=2).
    MockHttpServletRequest ipB = requestFrom("192.168.1.2");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = mock(FilterChain.class);
    filter.doFilter(ipB, response, chain);

    verify(chain, times(1)).doFilter(ipB, response);
    assertThat(response.getStatus()).isNotEqualTo(429);
  }

  @Test
  void xForwardedForHeaderUsedForClientIp() throws Exception {
    MockHttpServletRequest request = requestFrom("10.0.0.3");
    request.addHeader("X-Forwarded-For", "203.0.113.5, 10.0.0.1");

    MockHttpServletResponse response = new MockHttpServletResponse();
    filter.doFilter(request, response, mock(FilterChain.class));

    // The bucket should be keyed on the first IP in the forwarded header.
    assertThat(response.getStatus()).isNotEqualTo(429);
  }

  private static MockHttpServletRequest requestFrom(String remoteAddr) {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRemoteAddr(remoteAddr);
    return request;
  }
}
