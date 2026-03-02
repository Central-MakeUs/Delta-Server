package cmc.delta.global.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(LoggingOrder.HTTP_ACCESS_LOG_FILTER)
public class HttpAccessLogFilter extends OncePerRequestFilter {

	private static final Logger log = LoggerFactory.getLogger(HttpAccessLogFilter.class);

	private static final String ACCESS_LOG_FORMAT = "[HTTP] 요청 처리 완료 method={} path={} status={} durationMs={} ip={} ua={}";
	private static final String METRIC_NAME = "delta.http.server.duration";
	private static final String URI_UNKNOWN = "UNKNOWN";

	private static final int USER_AGENT_MAX_LEN = 200;

	private static final Set<String> SKIP_PATH_PREFIXES = Set.of(
		"/actuator", "/swagger", "/swagger-ui", "/v3/api-docs", "/favicon.ico");

	private final MeterRegistry meterRegistry;

	public HttpAccessLogFilter(MeterRegistry meterRegistry) {
		this.meterRegistry = meterRegistry;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return shouldSkipLogging(request.getRequestURI());
	}

	@Override
	protected void doFilterInternal(
		HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
		throws ServletException, IOException {

		StopWatch watch = startTiming();

		try {
			filterChain.doFilter(request, response);
		} finally {
			watch.stop();
			AccessLogContext ctx = createAccessLogContext(request, response, watch.getTotalTimeMillis());
			recordHttpMetric(ctx);
			writeAccessLog(ctx);
		}
	}

	private void recordHttpMetric(AccessLogContext ctx) {
		Timer.builder(METRIC_NAME)
			.description("HTTP request duration from access log filter")
			.tag("method", ctx.method())
			.tag("uri", resolveUriTag(ctx.path()))
			.tag("status", String.valueOf(ctx.status()))
			.register(meterRegistry)
			.record(ctx.durationMs(), TimeUnit.MILLISECONDS);
	}

	private String resolveUriTag(String path) {
		if (path == null || path.isBlank()) {
			return URI_UNKNOWN;
		}
		return path;
	}

	private boolean shouldSkipLogging(String path) {
		return SKIP_PATH_PREFIXES.stream().anyMatch(path::startsWith);
	}

	private StopWatch startTiming() {
		StopWatch watch = new StopWatch();
		watch.start();
		return watch;
	}

	private AccessLogContext createAccessLogContext(
		HttpServletRequest request, HttpServletResponse response, long durationMs) {

		return new AccessLogContext(
			request.getMethod(),
			buildSafePath(request),
			response.getStatus(),
			durationMs,
			resolveClientIp(request),
			resolveUserAgent(request));
	}

	/** 민감정보 유출 방지를 위해 queryString은 기본 로그에서 제외 */
	private String buildSafePath(HttpServletRequest request) {
		Object bestPattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
		if (bestPattern instanceof String pattern && !pattern.isBlank()) {
			return pattern;
		}
		return request.getRequestURI();
	}

	private void writeAccessLog(AccessLogContext ctx) {
		logByHttpStatus(
			ctx.status(),
			ACCESS_LOG_FORMAT,
			ctx.method(),
			ctx.path(),
			ctx.status(),
			ctx.durationMs(),
			ctx.clientIp(),
			ctx.userAgent());
	}

	private void logByHttpStatus(int status, String format, Object... args) {
		if (status >= LoggingConstants.HttpStatusBoundary.SERVER_ERROR_MIN) {
			log.error(format, args);
			return;
		}
		if (status >= LoggingConstants.HttpStatusBoundary.CLIENT_ERROR_MIN) {
			log.warn(format, args);
			return;
		}
		log.debug(format, args);
	}

	private String resolveClientIp(HttpServletRequest request) {
		String forwardedFor = request.getHeader(LoggingConstants.Header.X_FORWARDED_FOR);
		if (forwardedFor != null && !forwardedFor.isBlank()) {
			return forwardedFor.split(",")[0].trim();
		}

		String realIp = request.getHeader(LoggingConstants.Header.X_REAL_IP);
		if (realIp != null && !realIp.isBlank()) {
			return realIp.trim();
		}

		return request.getRemoteAddr();
	}

	private String resolveUserAgent(HttpServletRequest request) {
		String ua = request.getHeader(LoggingConstants.Header.USER_AGENT);
		if (ua == null || ua.isBlank()) {
			return LoggingConstants.Header.UNKNOWN;
		}
		return sanitize(ua, USER_AGENT_MAX_LEN);
	}

	private String sanitize(String raw, int maxLen) {
		String s = raw.replace("\n", " ").replace("\r", " ").trim();
		if (s.length() > maxLen) {
			return s.substring(0, maxLen);
		}
		return s;
	}

	private record AccessLogContext(
		String method, String path, int status, long durationMs, String clientIp, String userAgent) {
	}
}
