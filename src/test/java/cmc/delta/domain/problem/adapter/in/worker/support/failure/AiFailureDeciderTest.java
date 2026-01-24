package cmc.delta.domain.problem.adapter.in.worker.support.failure;

import static org.assertj.core.api.Assertions.*;

import cmc.delta.domain.problem.adapter.in.worker.exception.OcrTextEmptyException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

class AiFailureDeciderTest {

	private final AiFailureDecider decider = new AiFailureDecider();

	@Test
	@DisplayName("AI 실패 분기: ProblemScanWorkerException이면 non-retryable + 사유코드 그대로")
	void decide_whenWorkerException_thenNonRetryableWithReason() {
		// given
		Exception ex = new OcrTextEmptyException(10L);

		// when
		FailureDecision decision = decider.decide(ex);

		// then
		assertThat(decision.retryable()).isFalse();
		assertThat(decision.reasonCode()).isEqualTo(FailureReason.OCR_TEXT_EMPTY);
		assertThat(decision.retryAfterSeconds()).isNull();
	}

	@Test
	@DisplayName("AI 실패 분기: 네트워크 오류(ResourceAccessException)이면 retryable + AI_NETWORK_ERROR")
	void decide_whenNetworkError_thenRetryableNetworkReason() {
		// given
		Exception ex = new ResourceAccessException("timeout");

		// when
		FailureDecision decision = decider.decide(ex);

		// then
		assertThat(decision.retryable()).isTrue();
		assertThat(decision.reasonCode()).isEqualTo(FailureReason.AI_NETWORK_ERROR);
	}

	@Test
	@DisplayName("AI 실패 분기: 429 + Retry-After가 10이면 최소 60초로 보정")
	void decide_whenRateLimitWithSmallRetryAfter_thenUsesMinDelay() {
		// given
		HttpHeaders headers = new HttpHeaders();
		headers.add("Retry-After", "10");
		Exception ex = HttpClientErrorException.create(
			HttpStatus.TOO_MANY_REQUESTS,
			"too many",
			headers,
			new byte[0],
			null
		);

		// when
		FailureDecision decision = decider.decide(ex);

		// then
		assertThat(decision.retryable()).isTrue();
		assertThat(decision.reasonCode()).isEqualTo(FailureReason.AI_RATE_LIMIT);
		assertThat(decision.retryAfterSeconds()).isEqualTo(60L);
	}

	@Test
	@DisplayName("AI 실패 분기: 429 + Retry-After가 없으면 기본 180초")
	void decide_whenRateLimitWithoutRetryAfter_thenUsesDefaultDelay() {
		// given
		Exception ex = HttpClientErrorException.create(
			HttpStatus.TOO_MANY_REQUESTS,
			"too many",
			null,
			new byte[0],
			null
		);

		// when
		FailureDecision decision = decider.decide(ex);

		// then
		assertThat(decision.retryable()).isTrue();
		assertThat(decision.reasonCode()).isEqualTo(FailureReason.AI_RATE_LIMIT);
		assertThat(decision.retryAfterSeconds()).isEqualTo(180L);
	}

	@Test
	@DisplayName("AI 실패 분기: 5xx면 retryable + AI_CLIENT_5XX")
	void decide_when5xx_thenRetryable5xxReason() {
		// given
		Exception ex = new HttpServerErrorException(HttpStatus.BAD_GATEWAY);

		// when
		FailureDecision decision = decider.decide(ex);

		// then
		assertThat(decision.retryable()).isTrue();
		assertThat(decision.reasonCode()).isEqualTo(FailureReason.AI_CLIENT_5XX);
	}

	@Test
	@DisplayName("AI 실패 분기: 4xx면 non-retryable + AI_CLIENT_4XX")
	void decide_when4xx_thenNonRetryable4xxReason() {
		// given
		Exception ex = new HttpClientErrorException(HttpStatus.BAD_REQUEST);

		// when
		FailureDecision decision = decider.decide(ex);

		// then
		assertThat(decision.retryable()).isFalse();
		assertThat(decision.reasonCode()).isEqualTo(FailureReason.AI_CLIENT_4XX);
	}
}
