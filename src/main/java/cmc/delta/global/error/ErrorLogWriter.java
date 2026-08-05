package cmc.delta.global.error;

import cmc.delta.global.logging.LogSanitizer;
import cmc.delta.global.logging.LoggingConstants;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
public class ErrorLogWriter {

	private static final Logger log = LoggerFactory.getLogger(ErrorLogWriter.class);

	private static final int MSG_MAX_LEN = 300;
	private static final String ERROR_LOG_FORMAT = "error method={} path={} status={} code={} traceId={} exType={} exMsg={}";

	public void write(ErrorCode errorCode, Exception exception, HttpServletRequest request) {
		Object[] logArgs = buildLogArgs(errorCode, exception, request);

		// TRACE/DEBUG/ERROR에만 스택트레이스를 남기고, INFO/WARN은 요약만 남긴다.
		switch (errorCode.logLevel()) {
			case TRACE -> log.trace(ERROR_LOG_FORMAT, appendException(logArgs, exception));
			case DEBUG -> log.debug(ERROR_LOG_FORMAT, appendException(logArgs, exception));
			case INFO -> log.info(ERROR_LOG_FORMAT, logArgs);
			case WARN -> log.warn(ERROR_LOG_FORMAT, logArgs);
			case ERROR -> log.error(ERROR_LOG_FORMAT, appendException(logArgs, exception));
		}
	}

	private Object[] buildLogArgs(ErrorCode errorCode, Exception exception, HttpServletRequest request) {
		return new Object[] {
			request.getMethod(),
			request.getRequestURI(),
			errorCode.status().value(),
			errorCode.code(),
			MDC.get(LoggingConstants.Trace.MDC_KEY),
			exception.getClass().getSimpleName(),
			LogSanitizer.sanitize(exception.getMessage(), MSG_MAX_LEN)};
	}

	private Object[] appendException(Object[] logArgs, Exception exception) {
		Object[] extended = Arrays.copyOf(logArgs, logArgs.length + 1);
		extended[logArgs.length] = exception;
		return extended;
	}
}
