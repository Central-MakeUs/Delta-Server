package cmc.delta.global.logging;

public final class LoggingConstants {

    private LoggingConstants() {}

    public static final class Trace {
        private Trace() {}

        public static final String MDC_KEY = "traceId";
        public static final String HEADER = "X-Trace-Id";
    }

    public static final class Header {
        private Header() {}

        public static final String X_FORWARDED_FOR = "X-Forwarded-For";
        public static final String X_REAL_IP = "X-Real-IP";
        public static final String USER_AGENT = "User-Agent";
        public static final String UNKNOWN = "-";
    }

    public static final class HttpStatusBoundary {
        private HttpStatusBoundary() {}

        public static final int CLIENT_ERROR_MIN = 400;
        public static final int SERVER_ERROR_MIN = 500;
    }
}
