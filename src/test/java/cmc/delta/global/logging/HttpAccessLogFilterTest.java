package cmc.delta.global.logging;

import static org.assertj.core.api.Assertions.*;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class HttpAccessLogFilterTest {

	private final HttpAccessLogFilter filter = new HttpAccessLogFilter();

	@Test
	@DisplayName("shouldNotFilter: swagger 관련 경로면 filter를 타지 않는다")
	void shouldNotFilter_whenSwaggerPath_thenTrue() {
		// given
		MockHttpServletRequest req = new MockHttpServletRequest("GET", "/swagger-ui/index.html");

		// when
		boolean shouldNotFilter = filter.shouldNotFilter(req);

		// then
		assertThat(shouldNotFilter).isTrue();
	}

	@Test
	@DisplayName("doFilter: query string은 로그 path에 포함하지 않고, 5xx면 ERROR 레벨로 기록")
	void doFilter_whenQueryStringAndServerError_thenLogsWithoutQueryAtErrorLevel() throws Exception {
		// given
		Logger logger = (Logger) LoggerFactory.getLogger(HttpAccessLogFilter.class);
		ListAppender<ILoggingEvent> appender = new ListAppender<>();
		appender.start();
		logger.addAppender(appender);

		MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/test");
		req.setQueryString("q=1");
		req.addHeader(LoggingConstants.Header.X_FORWARDED_FOR, "1.1.1.1, 2.2.2.2");
		req.addHeader(LoggingConstants.Header.USER_AGENT, "ua\nline");

		MockHttpServletResponse res = new MockHttpServletResponse();
		FilterChain chain = new FilterChain() {
			@Override
			public void doFilter(ServletRequest request, ServletResponse response) {
				((MockHttpServletResponse) response).setStatus(500);
			}
		};

		// when
		filter.doFilter(req, res, chain);

		// then
		assertThat(appender.list).hasSize(1);
		ILoggingEvent event = appender.list.get(0);
		assertThat(event.getLevel()).isEqualTo(Level.ERROR);
		assertThat(event.getFormattedMessage())
			.contains("path=/api/v1/test")
			.doesNotContain("q=1")
			.contains("ip=1.1.1.1")
			.contains("ua=ua line");

		logger.detachAppender(appender);
		appender.stop();
	}
}
