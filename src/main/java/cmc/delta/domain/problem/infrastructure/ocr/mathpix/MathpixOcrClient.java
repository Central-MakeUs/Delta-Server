package cmc.delta.domain.problem.infrastructure.ocr.mathpix;

import cmc.delta.domain.problem.application.port.OcrClient;
import cmc.delta.domain.problem.application.port.OcrResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class MathpixOcrClient implements OcrClient {

	private final MathpixProperties props;
	private final ObjectMapper objectMapper;

	private final RestClient restClient = RestClient.builder().build();

	@Override
	public OcrResult recognizeMath(byte[] imageBytes, String filename) {
		MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
		form.add("file", new ByteArrayResource(imageBytes) {
			@Override public String getFilename() { return filename; }
		});

		// options_json: 문서 예시처럼 math_inline_delimiters / rm_spaces 등을 넣을 수 있음 :contentReference[oaicite:3]{index=3}
		String optionsJson = toOptionsJson();
		form.add("options_json", optionsJson);

		String body = restClient.post()
			.uri(props.baseUrl() + "/v3/text")
			.header("app_id", props.appId())
			.header("app_key", props.appKey())
			.contentType(MediaType.MULTIPART_FORM_DATA)
			.body(form)
			.retrieve()
			.body(String.class);

		return parse(body);
	}

	private String toOptionsJson() {
		try {
			// v3/text 응답의 text는 “Mathpix Markdown”이고, 수식 delimiters는 옵션으로 바꿀 수 있음 :contentReference[oaicite:4]{index=4}
			var options = new java.util.LinkedHashMap<String, Object>();
			options.put("math_inline_delimiters", new String[] {"$", "$"});
			options.put("rm_spaces", true);
			options.put("formats", new String[] {"text", "latex_styled"}); // 필요 최소만
			return objectMapper.writeValueAsString(options);
		} catch (Exception e) {
			throw new IllegalStateException("Mathpix options_json 생성 실패", e);
		}
	}

	private OcrResult parse(String body) {
		try {
			JsonNode root = objectMapper.readTree(body);

			String text = root.path("text").asText(null);
			String latexStyled = root.path("latex_styled").asText(null);
			Double confidence = root.hasNonNull("confidence") ? root.get("confidence").asDouble() : null;
			String requestId = root.path("request_id").asText(null);

			return new OcrResult(text, latexStyled, confidence, requestId, root);
		} catch (Exception e) {
			throw new IllegalStateException("Mathpix 응답 파싱 실패", e);
		}
	}
}
