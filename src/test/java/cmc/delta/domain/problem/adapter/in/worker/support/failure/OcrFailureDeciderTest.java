package cmc.delta.domain.problem.adapter.in.worker.support.failure;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

class OcrFailureDeciderTest {

	private final OcrFailureDecider decider = new OcrFailureDecider();

	@Test
	@DisplayName("OCR 실패 분기: 네트워크 오류(ResourceAccessException)이면 retryable + OCR_NETWORK_ERROR")
	void decide_whenNetworkError_thenRetryableNetworkReason() {
		// given
		Exception ex = new ResourceAccessException("timeout");

		// when
		FailureDecision decision = decider.decide(ex);

		// then
		assertThat(decision.retryable()).isTrue();
		assertThat(decision.reasonCode()).isEqualTo(FailureReason.OCR_NETWORK_ERROR);
	}

	@Test
	@DisplayName("OCR 실패 분기: 5xx면 retryable + OCR_CLIENT_5XX")
	void decide_when5xx_thenRetryable5xxReason() {
		// given
		Exception ex = new HttpServerErrorException(HttpStatus.BAD_GATEWAY);

		// when
		FailureDecision decision = decider.decide(ex);

		// then
		assertThat(decision.retryable()).isTrue();
		assertThat(decision.reasonCode()).isEqualTo(FailureReason.OCR_CLIENT_5XX);
	}

	@Test
	@DisplayName("OCR 실패 분기: 4xx면 non-retryable + OCR_CLIENT_4XX")
	void decide_when4xx_thenNonRetryable4xxReason() {
		// given
		Exception ex = new HttpClientErrorException(HttpStatus.BAD_REQUEST);

		// when
		FailureDecision decision = decider.decide(ex);

		// then
		assertThat(decision.retryable()).isFalse();
		assertThat(decision.reasonCode()).isEqualTo(FailureReason.OCR_CLIENT_4XX);
	}
}
