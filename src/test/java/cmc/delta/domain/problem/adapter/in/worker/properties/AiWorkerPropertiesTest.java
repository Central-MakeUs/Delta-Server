package cmc.delta.domain.problem.adapter.in.worker.properties;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AiWorkerPropertiesTest {

	@Test
	@DisplayName("AiWorkerProperties: backlogLogMinutes가 0 이하이면 5로 보정")
	void defaultsBacklogLogMinutes() {
		// when
		AiWorkerProperties p = new AiWorkerProperties(1L, 1, 1L, 1, 0);

		// then
		assertThat(p.backlogLogMinutes()).isEqualTo(5);
	}
}
