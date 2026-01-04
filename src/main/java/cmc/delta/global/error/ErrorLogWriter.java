package cmc.delta.global.error;

import cmc.delta.global.logging.LoggingConstants;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
public class ErrorLogWriter {

	private static final Logger log = LoggerFactory.getLogger(ErrorLogWriter.class);

	private static final int MSG_MAX_LEN = 300;

	public void write(ErrorCode errorCode, Exception exception, HttpServletRequest request) {
		String method = request.getMethod();
		String path = request.getRequestURI();
		int httpStatus = errorCode.status().value();

		String traceId = MDC.get(LoggingConstants.Trace.MDC_KEY);

		String exType = exception.getClass().getSimpleName();
		String exMsg = sanitize(exception.getMessage(), MSG_MAX_LEN);

		switch (errorCode.logLevel()) {
			case TRACE -> log.trace(
				"error method={} path={} status={} code={} traceId={} exType={} exMsg={}",
				method,
				path,
				httpStatus,
				errorCode.code(),
				traceId,
				exType,
				exMsg,
				exception);

			case DEBUG -> log.debug(
				"error method={} path={} status={} code={} traceId={} exType={} exMsg={}",
				method,
				path,
				httpStatus,
				errorCode.code(),
				traceId,
				exType,
				exMsg,
				exception);

			case INFO -> log.info(
				"error method={} path={} status={} code={} traceId={} exType={} exMsg={}",
				method,
				path,
				httpStatus,
				errorCode.code(),
				traceId,
				exType,
				exMsg);

			case WARN -> log.warn(
				"error method={} path={} status={} code={} traceId={} exType={} exMsg={}",
				method,
				path,
				httpStatus,
				errorCode.code(),
				traceId,
				exType,
				exMsg);

			case ERROR -> log.error(
				"error method={} path={} status={} code={} traceId={} exType={} exMsg={}",
				method,
				path,
				httpStatus,
				errorCode.code(),
				traceId,
				exType,
				exMsg,
				exception);
		}
	}

	private String sanitize(String raw, int maxLen) {
		if (raw == null)
			return LoggingConstants.Header.UNKNOWN;

		String s = raw.replace("\n", " ").replace("\r", " ").trim();
		if (s.length() > maxLen)
			return s.substring(0, maxLen);
		return s;
	}
}
