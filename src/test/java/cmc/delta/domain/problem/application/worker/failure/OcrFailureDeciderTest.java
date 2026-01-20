package cmc.delta.domain.problem.application.worker.failure;

import static org.assertj.core.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import cmc.delta.domain.problem.adapter.in.worker.support.failure.FailureDecision;
import cmc.delta.domain.problem.adapter.in.worker.support.failure.FailureReason;
import cmc.delta.domain.problem.adapter.in.worker.support.failure.OcrFailureDecider;

class OcrFailureDeciderTest {

	private final OcrFailureDecider sut = new OcrFailureDecider();

	@Test
	@DisplayName("OCR 429는 Retry-After가 없으면 기본 180초 delay를 사용한다.")
	void rateLimit_withoutRetryAfter_usesDefault180() {
		// given
		HttpClientErrorException ex = HttpClientErrorException.create(
			HttpStatus.TOO_MANY_REQUESTS,
			"429",
			new HttpHeaders(),
			"{}".getBytes(StandardCharsets.UTF_8),
			StandardCharsets.UTF_8
		);

		// when
		FailureDecision decision = sut.decide(ex);

		// then
		assertThat(decision.reasonCode()).isEqualTo(FailureReason.OCR_RATE_LIMIT);
		assertThat(decision.retryable()).isTrue();
		assertThat(decision.retryAfterSeconds()).isEqualTo(180L);
	}

	@Test
	@DisplayName("OCR 429는 Retry-After가 너무 작으면(예:10) 최소 60초로 보정한다.")
	void rateLimit_smallRetryAfter_isClampedTo60() {
		// given
		HttpHeaders headers = new HttpHeaders();
		headers.add("Retry-After", "10");

		HttpClientErrorException ex = HttpClientErrorException.create(
			HttpStatus.TOO_MANY_REQUESTS,
			"429",
			headers,
			null,
			null
		);

		// when
		FailureDecision decision = sut.decide(ex);

		// then
		assertThat(decision.reasonCode()).isEqualTo(FailureReason.OCR_RATE_LIMIT);
		assertThat(decision.retryable()).isTrue();
		assertThat(decision.retryAfterSeconds()).isEqualTo(60L);
	}

	@Test
	@DisplayName("OCR 429는 Retry-After가 충분히 크면(예:300) 그대로 사용한다.")
	void rateLimit_largeRetryAfter_usesValue() {
		// given
		HttpHeaders headers = new HttpHeaders();
		headers.add("Retry-After", "300");

		HttpClientErrorException ex = HttpClientErrorException.create(
			HttpStatus.TOO_MANY_REQUESTS,
			"429",
			headers,
			null,
			null
		);

		// when
		FailureDecision decision = sut.decide(ex);

		// then
		assertThat(decision.reasonCode()).isEqualTo(FailureReason.OCR_RATE_LIMIT);
		assertThat(decision.retryable()).isTrue();
		assertThat(decision.retryAfterSeconds()).isEqualTo(300L);
	}
}
