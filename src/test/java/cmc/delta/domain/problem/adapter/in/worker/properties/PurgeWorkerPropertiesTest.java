package cmc.delta.domain.problem.adapter.in.worker.properties;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PurgeWorkerPropertiesTest {

	@Test
	@DisplayName("purge worker properties: 값이 비어있으면 안전한 기본값으로 보정된다")
	void normalizesDefaults() {
		PurgeWorkerProperties props = new PurgeWorkerProperties(0L, 0, 0L, 0, 0, 0);

		assertThat(props.fixedDelayMs()).isPositive();
		assertThat(props.batchSize()).isPositive();
		assertThat(props.lockLeaseSeconds()).isPositive();
		assertThat(props.concurrency()).isPositive();
		assertThat(props.backlogLogMinutes()).isPositive();
		assertThat(props.retentionDays()).isPositive();
	}
}
