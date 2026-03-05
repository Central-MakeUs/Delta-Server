package cmc.delta.domain.problem.adapter.out.ai.gemini;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GeminiPropertiesTest {

	@Test
	@DisplayName("GeminiProperties: baseUrl/model이 blank면 기본값을 사용")
	void defaults_whenBlank() {
		// when
		GeminiProperties p = new GeminiProperties(" ", "k", "  ", " ");

		// then
		assertThat(p.baseUrl()).isEqualTo("https://generativelanguage.googleapis.com");
		assertThat(p.model()).isEqualTo("gemini-2.5-flash-lite");
		assertThat(p.solveModel()).isEqualTo("gemini-2.5-flash-lite");
		assertThat(p.apiKey()).isEqualTo("k");
	}

	@Test
	@DisplayName("GeminiProperties: 값이 있으면 유지")
	void keeps_whenProvided() {
		// when
		GeminiProperties p = new GeminiProperties("https://x", "k", "m", "s");

		// then
		assertThat(p.baseUrl()).isEqualTo("https://x");
		assertThat(p.model()).isEqualTo("m");
		assertThat(p.solveModel()).isEqualTo("s");
	}
}
