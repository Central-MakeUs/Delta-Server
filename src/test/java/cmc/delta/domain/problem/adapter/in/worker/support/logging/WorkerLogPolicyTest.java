package cmc.delta.domain.problem.adapter.in.worker.support.logging;

import static org.assertj.core.api.Assertions.*;

import cmc.delta.domain.problem.adapter.in.worker.support.failure.FailureDecision;
import cmc.delta.domain.problem.adapter.in.worker.support.failure.FailureReason;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientResponseException;

class WorkerLogPolicyTest {

	private final WorkerLogPolicy policy = new WorkerLogPolicy();

	@Test
	@DisplayName("로그 정책: 4xx RestClientResponseException이면 stacktrace를 suppress")
	void shouldSuppressStacktrace_when4xx_thenTrue() {
		// given
		RestClientResponseException ex = new HttpClientErrorException(HttpStatus.BAD_REQUEST);

		// when
		boolean suppress = policy.shouldSuppressStacktrace(ex);

		// then
		assertThat(suppress).isTrue();
	}

	@Test
	@DisplayName("로그 정책: 일반 예외면 suppress하지 않음")
	void shouldSuppressStacktrace_whenNonRestClient_thenFalse() {
		// when
		boolean suppress = policy.shouldSuppressStacktrace(new RuntimeException("x"));

		// then
		assertThat(suppress).isFalse();
	}

	@Test
	@DisplayName("로그 정책: FailureDecision의 reasonCode를 code 문자열로 반환")
	void reasonCode_returnsCode() {
		// given
		FailureDecision decision = FailureDecision.retryable(FailureReason.AI_FAILED);

		// when
		String code = policy.reasonCode(decision);

		// then
		assertThat(code).isEqualTo(FailureReason.AI_FAILED.code());
	}
}
