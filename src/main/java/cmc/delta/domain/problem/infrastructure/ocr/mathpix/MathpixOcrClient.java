package cmc.delta.domain.problem.infrastructure.ocr.mathpix;

import cmc.delta.domain.problem.application.port.OcrClient;
import cmc.delta.domain.problem.application.port.OcrResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
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
	public OcrResult recognize(byte[] imageBytes, String filename) {
		MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
		form.add("file", new ByteArrayResource(imageBytes) {
			@Override public String getFilename() { return filename; }
		});
		form.add("options_json", buildOptionsJson());

		String response = restClient.post()
			.uri(props.baseUrl() + "/v3/text")
			.header("app_id", props.appId())
			.header("app_key", props.appKey())
			.contentType(MediaType.MULTIPART_FORM_DATA)
			.body(form)
			.retrieve()
			.body(String.class);

		return parse(response);
	}

	private String buildOptionsJson() {
		try {
			LinkedHashMap<String, Object> options = new LinkedHashMap<>();
			options.put("math_inline_delimiters", new String[] {"$", "$"});
			options.put("rm_spaces", true);
			options.put("formats", new String[] {"text"}); // 필요 최소
			return objectMapper.writeValueAsString(options);
		} catch (Exception e) {
			throw new IllegalStateException("Mathpix options_json 생성 실패", e);
		}
	}

	private OcrResult parse(String response) {
		try {
			JsonNode root = objectMapper.readTree(response);
			String text = root.path("text").asText(null); // Mathpix Markdown
			return new OcrResult(text, root.toString());
		} catch (Exception e) {
			throw new IllegalStateException("Mathpix 응답 파싱 실패", e);
		}
	}
}
