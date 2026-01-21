package cmc.delta.domain.problem.application.command.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TypeCandidatesParser {

	private final ObjectMapper objectMapper;

	/**
	 * Gemini 스키마:
	 * type_candidates: [{"id":"...", "score":0.0}, ...]
	 */
	public List<TypeCandidate> parseTypeCandidates(String typeCandidatesJson) {
		if (typeCandidatesJson == null || typeCandidatesJson.isBlank())
			return List.of();

		try {
			JsonNode root = objectMapper.readTree(typeCandidatesJson);
			if (!root.isArray())
				return List.of();

			List<TypeCandidate> list = new ArrayList<>();
			for (JsonNode n : root) {
				String id = text(n, "id");
				BigDecimal score = decimal(n, "score");
				if (id == null || id.isBlank())
					continue;
				list.add(new TypeCandidate(id, score));
			}

			// score 내림차순
			list.sort(
				Comparator.comparing(TypeCandidate::score, Comparator.nullsLast(Comparator.naturalOrder())).reversed());
			return list;
		} catch (Exception e) {
			return List.of();
		}
	}

	private String text(JsonNode node, String key) {
		JsonNode v = node.get(key);
		return v != null && v.isTextual() ? v.asText() : null;
	}

	private BigDecimal decimal(JsonNode node, String key) {
		JsonNode v = node.get(key);
		if (v == null)
			return null;
		if (v.isNumber())
			return v.decimalValue();
		if (v.isTextual()) {
			try {
				return new BigDecimal(v.asText());
			} catch (Exception ignored) {
			}
		}
		return null;
	}

	public record TypeCandidate(String typeId, BigDecimal score) {
	}
}
