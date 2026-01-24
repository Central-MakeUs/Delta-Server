package cmc.delta.domain.problem.application.command.support;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TypeCandidatesParserTest {

	private final TypeCandidatesParser parser = new TypeCandidatesParser(new ObjectMapper());

	@Test
	@DisplayName("typeCandidates 파싱: json이 null/blank면 빈 리스트")
	void parse_whenBlank_thenEmpty() {
		// when
		List<TypeCandidatesParser.TypeCandidate> out1 = parser.parseTypeCandidates(null);
		List<TypeCandidatesParser.TypeCandidate> out2 = parser.parseTypeCandidates("  ");

		// then
		assertThat(out1).isEmpty();
		assertThat(out2).isEmpty();
	}

	@Test
	@DisplayName("typeCandidates 파싱: 유효하지 않은 json이면 빈 리스트")
	void parse_whenInvalidJson_thenEmpty() {
		// when
		List<TypeCandidatesParser.TypeCandidate> out = parser.parseTypeCandidates("{");

		// then
		assertThat(out).isEmpty();
	}

	@Test
	@DisplayName("typeCandidates 파싱: root가 배열이 아니면 빈 리스트")
	void parse_whenNotArray_thenEmpty() {
		// when
		List<TypeCandidatesParser.TypeCandidate> out = parser.parseTypeCandidates("{\"id\":\"T1\"}");

		// then
		assertThat(out).isEmpty();
	}

	@Test
	@DisplayName("typeCandidates 파싱: id가 blank면 제외하고 score 기준 내림차순 정렬")
	void parse_whenValid_thenFiltersAndSorts() {
		// given
		String json = "[" +
			"{\"id\":\"T1\",\"score\":0.2}," +
			"{\"id\":\"\",\"score\":0.9}," +
			"{\"id\":\"T2\",\"score\":\"0.9\"}," +
			"{\"id\":\"T3\"}" +
			"]";

		// when
		List<TypeCandidatesParser.TypeCandidate> out = parser.parseTypeCandidates(json);

		// then
		assertThat(out).hasSize(3);
		// NOTE: 구현은 nullsLast(...) 후 reversed()라서 null score가 먼저 온다.
		assertThat(out.get(0).typeId()).isEqualTo("T3");
		assertThat(out.get(1).typeId()).isEqualTo("T2");
		assertThat(out.get(2).typeId()).isEqualTo("T1");
	}
}
