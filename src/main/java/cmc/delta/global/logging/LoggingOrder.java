package cmc.delta.global.logging;

import org.springframework.core.Ordered;

public final class LoggingOrder {

	private LoggingOrder() {}

	public static final int TRACE_ID_FILTER = Ordered.HIGHEST_PRECEDENCE;
	public static final int HTTP_ACCESS_LOG_FILTER = TRACE_ID_FILTER + 10;
}
