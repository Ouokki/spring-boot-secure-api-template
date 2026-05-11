package com.ouokki.secureapi.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CorrelationIdFilterTest {

  CorrelationIdFilter filter = new CorrelationIdFilter();

  @Test
  void generatesCorrelationIdWhenAbsentFromRequest() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, mock(FilterChain.class));

    assertThat(response.getHeader(CorrelationIdFilter.HEADER)).isNotBlank();
  }

  @Test
  void propagatesExistingCorrelationIdFromRequest() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(CorrelationIdFilter.HEADER, "my-trace-id");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, mock(FilterChain.class));

    assertThat(response.getHeader(CorrelationIdFilter.HEADER)).isEqualTo("my-trace-id");
  }

  @Test
  void mdcKeyIsPopulatedDuringRequestAndClearedAfter() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();

    final String[] mdcValueDuringRequest = {null};
    FilterChain chain =
        (req, resp) -> mdcValueDuringRequest[0] = MDC.get(CorrelationIdFilter.MDC_KEY);

    filter.doFilter(request, response, chain);

    assertThat(mdcValueDuringRequest[0]).isNotBlank();
    assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull(); // cleared after request
  }

  @Test
  void correlationIdInResponseMatchesMdcValueDuringRequest() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    final String[] capturedMdc = {null};

    filter.doFilter(
        request, response, (req, resp) -> capturedMdc[0] = MDC.get(CorrelationIdFilter.MDC_KEY));

    assertThat(response.getHeader(CorrelationIdFilter.HEADER)).isEqualTo(capturedMdc[0]);
  }
}
