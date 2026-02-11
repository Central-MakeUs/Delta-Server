package cmc.delta.domain.problem.adapter.in.worker.support.ocr;

import cmc.delta.domain.problem.application.port.out.ocr.dto.OcrSignalSummary;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class LineDataSignalExtractor {

	private static final String JSON_LINE_DATA = "line_data";
	private static final String JSON_TYPE = "type";

	private static final OcrSignalSummary EMPTY = new OcrSignalSummary(0, 0, 0, 0);

	private final ObjectMapper objectMapper;

	public LineDataSignalExtractor(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public OcrSignalSummary extract(String ocrRawJson) {
		if (ocrRawJson == null || ocrRawJson.isBlank()) {
			return EMPTY;
		}
		try {
			JsonNode root = objectMapper.readTree(ocrRawJson);
			JsonNode lines = root.path(JSON_LINE_DATA);
			if (!lines.isArray()) {
				return EMPTY;
			}

			int math = 0;
			int text = 0;
			int code = 0;
			int pseudocode = 0;

			for (JsonNode line : lines) {
				String type = normalizeType(line.path(JSON_TYPE).asText(""));
				switch (type) {
					case "math" -> math++;
					case "text" -> text++;
					case "code" -> code++;
					case "pseudocode" -> pseudocode++;
					default -> {
					}
				}
			}

			return new OcrSignalSummary(math, text, code, pseudocode);
		} catch (Exception e) {
			return EMPTY;
		}
	}

	private String normalizeType(String type) {
		return type == null ? "" : type.trim().toLowerCase();
	}
}
