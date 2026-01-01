package cmc.delta.global.error;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ErrorLogWriter {

    private static final Logger log = LoggerFactory.getLogger(ErrorLogWriter.class);

    public void write(ErrorCode errorCode, Exception exception, HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();

        switch (errorCode.logLevel()) {
            case TRACE -> log.trace("[{} {}] code={} msg={}", method, path, errorCode.code(), exception.getMessage(), exception);
            case DEBUG -> log.debug("[{} {}] code={} msg={}", method, path, errorCode.code(), exception.getMessage(), exception);

            case INFO  -> log.info("[{} {}] code={} msg={}", method, path, errorCode.code(), exception.getMessage());
            case WARN  -> log.warn("[{} {}] code={} msg={}", method, path, errorCode.code(), exception.getMessage());

            case ERROR -> log.error("[{} {}] code={} msg={}", method, path, errorCode.code(), exception.getMessage(), exception);
        }
    }
}
