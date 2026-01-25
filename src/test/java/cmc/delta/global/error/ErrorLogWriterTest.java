package cmc.delta.global.error;

import static org.assertj.core.api.Assertions.*;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import cmc.delta.global.logging.LoggingConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;

class ErrorLogWriterTest {

	private final ErrorLogWriter writer = new ErrorLogWriter();

	@Test
	@DisplayName("write: 예외 메시지는 개행 제거/trim 후 maxLen(300)으로 잘라서 기록")
	void write_whenMessageHasNewlineAndTooLong_thenSanitizesAndTruncates() {
		// given
		Logger logger = (Logger) LoggerFactory.getLogger(ErrorLogWriter.class);
		ListAppender<ILoggingEvent> appender = new ListAppender<>();
		appender.start();
		logger.addAppender(appender);

		String longMsg = ("a".repeat(400)) + "\n";
		RuntimeException ex = new RuntimeException(longMsg);
		MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/v1/test");
		MDC.put(LoggingConstants.Trace.MDC_KEY, "t1");

		// when
		writer.write(ErrorCode.INVALID_REQUEST, ex, req);

		// then
		assertThat(appender.list).hasSize(1);
		String formatted = appender.list.get(0).getFormattedMessage();
		assertThat(formatted).contains("traceId=t1");
		assertThat(formatted).contains("exMsg=" + "a".repeat(300));
		assertThat(formatted).doesNotContain("\n");

		logger.detachAppender(appender);
		appender.stop();
		MDC.remove(LoggingConstants.Trace.MDC_KEY);
	}

	@Test
	@DisplayName("write: 예외 메시지가 null이면 exMsg는 UNKNOWN(-)로 기록")
	void write_whenMessageNull_thenUsesUnknown() {
		// given
		Logger logger = (Logger) LoggerFactory.getLogger(ErrorLogWriter.class);
		ListAppender<ILoggingEvent> appender = new ListAppender<>();
		appender.start();
		logger.addAppender(appender);

		RuntimeException ex = new RuntimeException((String) null);
		MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/test");

		// when
		writer.write(ErrorCode.INVALID_REQUEST, ex, req);

		// then
		assertThat(appender.list).hasSize(1);
		String formatted = appender.list.get(0).getFormattedMessage();
		assertThat(formatted).contains("exMsg=" + LoggingConstants.Header.UNKNOWN);

		logger.detachAppender(appender);
		appender.stop();
	}
}
