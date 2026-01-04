package cmc.delta.global.logging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j(topic = "SECURITY_AUDIT")
@Component
public class TokenAuditLogger {

	public void invalidateAll(long userId, String sessionId, boolean blacklisted, long blacklistTtlSeconds) {
		log.info(
			"event=token.invalidate_all result=success userId={} sessionId={} blacklisted={} blacklistTtlSeconds={}",
			userId, sessionId, blacklisted, blacklistTtlSeconds
		);
	}

	public void reissueFailed(long userId, String sessionId, String rotateResult, String errorCode) {
		log.warn(
			"event=token.reissue result=fail userId={} sessionId={} rotateResult={} errorCode={}",
			userId, sessionId, rotateResult, errorCode
		);
	}

	public void refreshMismatch(long userId, String sessionId, String action, String errorCode) {
		log.warn(
			"event=token.refresh_mismatch result=fail action={} userId={} sessionId={} errorCode={}",
			action, userId, sessionId, errorCode
		);
	}

	public void blacklistFailed(long userId, String sessionId, String action, String reason) {
		log.warn(
			"event=token.blacklist result=fail action={} userId={} sessionId={} reason={}",
			action, userId, sessionId, reason
		);
	}
}
