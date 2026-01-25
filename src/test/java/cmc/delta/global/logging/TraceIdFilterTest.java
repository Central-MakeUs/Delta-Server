package cmc.delta.global.logging;

import static org.assertj.core.api.Assertions.*;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class TraceIdFilterTest {

	private final TraceIdFilter filter = new TraceIdFilter();

	@Test
	@DisplayName("traceId: 요청 헤더에 유효한 값이 있으면 그 값을 MDC/응답 헤더로 사용")
	void doFilter_whenIncomingTraceIdValid_thenUsesIt() throws Exception {
		// given
		MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/test");
		req.addHeader(LoggingConstants.Trace.HEADER, "trace_123");
		MockHttpServletResponse res = new MockHttpServletResponse();

		CapturingChain chain = new CapturingChain();

		// when
		filter.doFilter(req, res, chain);

		// then
		assertThat(chain.traceIdDuringChain).isEqualTo("trace_123");
		assertThat(res.getHeader(LoggingConstants.Trace.HEADER)).isEqualTo("trace_123");
		assertThat(MDC.get(LoggingConstants.Trace.MDC_KEY)).isNull();
	}

	@Test
	@DisplayName("traceId: 요청 헤더가 invalid면 새 traceId를 생성해서 사용")
	void doFilter_whenIncomingTraceIdInvalid_thenGeneratesNew() throws Exception {
		// given
		MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/test");
		req.addHeader(LoggingConstants.Trace.HEADER, "bad trace id");
		MockHttpServletResponse res = new MockHttpServletResponse();

		CapturingChain chain = new CapturingChain();

		// when
		filter.doFilter(req, res, chain);

		// then
		assertThat(chain.traceIdDuringChain)
			.isNotBlank()
			.matches("^[a-f0-9]{32}$");
		assertThat(res.getHeader(LoggingConstants.Trace.HEADER)).isEqualTo(chain.traceIdDuringChain);
		assertThat(MDC.get(LoggingConstants.Trace.MDC_KEY)).isNull();
	}

	private static class CapturingChain implements FilterChain {
		private String traceIdDuringChain;

		@Override
		public void doFilter(ServletRequest request, ServletResponse response) {
			traceIdDuringChain = MDC.get(LoggingConstants.Trace.MDC_KEY);
		}
	}
}
