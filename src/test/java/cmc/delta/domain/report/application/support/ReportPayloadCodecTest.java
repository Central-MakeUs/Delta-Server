package cmc.delta.domain.report.application.support;

import static org.assertj.core.api.Assertions.*;

import cmc.delta.domain.report.application.dto.ReportAggregate;
import cmc.delta.domain.report.application.dto.ReportAggregate.WeakArea;
import cmc.delta.domain.report.application.dto.ReportNarrative;
import cmc.delta.domain.report.application.dto.ReportNarrative.UnitComment;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReportPayloadCodecTest {

	private final ReportPayloadCodec codec = new ReportPayloadCodec(new ObjectMapper());

	@Test
	@DisplayName("집계/서술 record(중첩 포함)를 JSON으로 왕복시킨다")
	void roundTripsRecords() {
		ReportAggregate aggregate = new ReportAggregate(12, 4, 8,
			List.of(new WeakArea("u1", "함수", 6, 5, 0.833, 9.17)),
			List.of(new WeakArea("t1", "개념이해", 5, 5, 1.0, 10.0)));
		ReportNarrative narrative = new ReportNarrative(
			"진단", List.of(new UnitComment("함수", "코멘트")), "학습방향", List.of("추천1", "추천2"));

		assertThat(codec.read(codec.write(aggregate), ReportAggregate.class)).isEqualTo(aggregate);
		assertThat(codec.read(codec.write(narrative), ReportNarrative.class)).isEqualTo(narrative);
	}

	@Test
	@DisplayName("null JSON은 null로 복원한다 (미완료 리포트)")
	void readsNullAsNull() {
		assertThat(codec.read(null, ReportAggregate.class)).isNull();
	}
}
