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

	private static final String FIELD_ID = "id";
	private static final String FIELD_SCORE = "score";

	private final ObjectMapper objectMapper;

	/**
	 * Gemini 스키마:
	 * type_candidates: [{"id":"...", "score":0.0}, ...]
	 */
	public List<TypeCandidate> parseTypeCandidates(String typeCandidatesJson) {
		if (typeCandidatesJson == null || typeCandidatesJson.isBlank()) {
			return List.of();
		}

		JsonNode root = parseArrayOrNull(typeCandidatesJson);
		if (root == null) {
			return List.of();
		}

		List<TypeCandidate> candidates = readCandidates(root);
		return sortByScoreDesc(candidates);
	}

	private JsonNode parseArrayOrNull(String json) {
		try {
			JsonNode root = objectMapper.readTree(json);
			return root.isArray() ? root : null;
		} catch (Exception e) {
			return null;
		}
	}

	private List<TypeCandidate> readCandidates(JsonNode root) {
		List<TypeCandidate> list = new ArrayList<>();
		for (JsonNode node : root) {
			TypeCandidate candidate = toCandidate(node);
			if (candidate != null) {
				list.add(candidate);
			}
		}
		return list;
	}

	private TypeCandidate toCandidate(JsonNode node) {
		String id = text(node, FIELD_ID);
		if (id == null || id.isBlank()) {
			return null;
		}
		BigDecimal score = decimal(node, FIELD_SCORE);
		return new TypeCandidate(id, score);
	}

	private List<TypeCandidate> sortByScoreDesc(List<TypeCandidate> candidates) {
		if (candidates.size() <= 1) {
			return candidates;
		}
		candidates.sort(
			Comparator.comparing(TypeCandidate::score, Comparator.nullsLast(Comparator.naturalOrder())).reversed());
		return candidates;
	}

	private String text(JsonNode node, String key) {
		JsonNode v = node.get(key);
		return v != null && v.isTextual() ? v.asText() : null;
	}

	private BigDecimal decimal(JsonNode node, String key) {
		JsonNode v = node.get(key);
		if (v == null) {
			return null;
		}
		if (v.isNumber()) {
			return v.decimalValue();
		}
		if (v.isTextual()) {
			try {
				return new BigDecimal(v.asText());
			} catch (Exception ignored) {
				return null;
			}
		}
		return null;
	}

	public record TypeCandidate(String typeId, BigDecimal score) {
	}
}
