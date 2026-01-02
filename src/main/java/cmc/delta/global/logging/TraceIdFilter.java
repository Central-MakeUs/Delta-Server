package cmc.delta.global.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
@Order(LoggingOrder.TRACE_ID_FILTER)
public class TraceIdFilter extends OncePerRequestFilter {

    private static final Pattern TRACE_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{1,64}$");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String traceId = resolveTraceId(request);

        try {
            bindTraceId(traceId);
            exposeTraceIdHeader(response, traceId);
            filterChain.doFilter(request, response);
        } finally {
            unbindTraceId();
        }
    }

    private String resolveTraceId(HttpServletRequest request) {
        String incoming = extractTraceIdHeader(request);
        if (isValidTraceId(incoming)) {
            return incoming;
        }
        return generateTraceId();
    }

    private String extractTraceIdHeader(HttpServletRequest request) {
        String value = request.getHeader(LoggingConstants.Trace.HEADER);
        return value == null ? null : value.trim();
    }

    private boolean isValidTraceId(String traceId) {
        return traceId != null && !traceId.isBlank() && TRACE_ID_PATTERN.matcher(traceId).matches();
    }

    private String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private void bindTraceId(String traceId) {
        MDC.put(LoggingConstants.Trace.MDC_KEY, traceId);
    }

    private void exposeTraceIdHeader(HttpServletResponse response, String traceId) {
        response.setHeader(LoggingConstants.Trace.HEADER, traceId);
    }

    private void unbindTraceId() {
        MDC.remove(LoggingConstants.Trace.MDC_KEY);
    }
}
