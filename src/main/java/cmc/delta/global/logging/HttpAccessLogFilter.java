package cmc.delta.global.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

@Component
@Order(LoggingOrder.HTTP_ACCESS_LOG_FILTER)
public class HttpAccessLogFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(HttpAccessLogFilter.class);

    private static final String ACCESS_LOG_FORMAT = "[HTTP] {} {} | status={} | {}ms | ip={} | ua={}";

    private static final Set<String> SKIP_PATH_PREFIXES = Set.of(
            "/actuator",
            "/swagger",
            "/v3/api-docs",
            "/favicon.ico"
    );

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return shouldSkipLogging(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        StopWatch watch = startTiming();

        try {
            filterChain.doFilter(request, response);
        } finally {
            watch.stop();
            AccessLogContext ctx = createAccessLogContext(request, response, watch.getTotalTimeMillis());
            writeAccessLog(ctx);
        }
    }

    private boolean shouldSkipLogging(String path) {
        return SKIP_PATH_PREFIXES.stream().anyMatch(path::startsWith);
    }

    private StopWatch startTiming() {
        StopWatch watch = new StopWatch();
        watch.start();
        return watch;
    }

    private AccessLogContext createAccessLogContext(HttpServletRequest request, HttpServletResponse response, long durationMs) {
        return new AccessLogContext(
                request.getMethod(),
                buildFullPath(request),
                response.getStatus(),
                durationMs,
                resolveClientIp(request),
                resolveUserAgent(request)
        );
    }

    private String buildFullPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        String query = request.getQueryString();
        return (query == null || query.isBlank()) ? path : path + "?" + query;
    }

    private void writeAccessLog(AccessLogContext ctx) {
        logByHttpStatus(ctx.status(),
                ACCESS_LOG_FORMAT,
                ctx.method(), ctx.fullPath(), ctx.status(), ctx.durationMs(), ctx.clientIp(), ctx.userAgent()
        );
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
        log.info(format, args);
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
        return (ua == null || ua.isBlank()) ? LoggingConstants.Header.UNKNOWN : ua;
    }

    private record AccessLogContext(
            String method,
            String fullPath,
            int status,
            long durationMs,
            String clientIp,
            String userAgent
    ) {}
}
