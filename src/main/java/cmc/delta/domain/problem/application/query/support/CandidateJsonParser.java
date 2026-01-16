package cmc.delta.domain.problem.application.query.support;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CandidateJsonParser {

	private final ObjectMapper objectMapper;

	public List<CandidateIdScore> parse(String json) {
		if (json == null || json.isBlank()) {
			return Collections.emptyList();
		}
		try {
			return objectMapper.readValue(json, new TypeReference<List<CandidateIdScore>>() {});
		} catch (Exception e) {
			return Collections.emptyList();
		}
	}
}
