package cmc.delta.domain.problem.adapter.out.ocr.mathpix;

import cmc.delta.domain.problem.application.port.out.ocr.OcrClient;
import cmc.delta.domain.problem.application.port.out.ocr.dto.OcrResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class MathpixOcrClient implements OcrClient {

	private static final String PATH_TEXT = "/v3/text";
	private static final String HEADER_APP_ID = "app_id";
	private static final String HEADER_APP_KEY = "app_key";

	private static final String FORM_FILE = "file";
	private static final String FORM_OPTIONS_JSON = "options_json";

	private static final String JSON_TEXT = "text";
	private static final String JSON_LATEX_STYLED = "latex_styled";

	private final MathpixProperties props;
	private final ObjectMapper objectMapper;
	private final RestClient restClient;

	private final String optionsJson;

	public MathpixOcrClient(MathpixProperties props, ObjectMapper objectMapper, RestClient restClient) {
		this.props = props;
		this.objectMapper = objectMapper;
		this.restClient = restClient;
		this.optionsJson = buildOptionsJsonCached();
	}

	@Override
	public OcrResult recognize(byte[] imageBytes, String filename) {
		try {
			MultiValueMap<String, Object> form = buildForm(imageBytes, filename);

			String response = restClient.post()
				.uri(props.baseUrl() + PATH_TEXT)
				.header(HEADER_APP_ID, props.appId())
				.header(HEADER_APP_KEY, props.appKey())
				.contentType(MediaType.MULTIPART_FORM_DATA)
				.body(form)
				.retrieve()
				.body(String.class);

			return parse(response);

		} catch (RestClientResponseException e) {
			throw MathpixOcrException.externalCallFailed(e);
		} catch (MathpixOcrException e) {
			throw e;
		} catch (Exception e) {
			throw MathpixOcrException.responseParseFailed(e);
		}
	}

	private MultiValueMap<String, Object> buildForm(byte[] imageBytes, String filename) {
		MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
		form.add(FORM_FILE, new ByteArrayResource(imageBytes) {
			@Override
			public String getFilename() {
				return filename;
			}
		});
		form.add(FORM_OPTIONS_JSON, optionsJson);
		return form;
	}

	private String buildOptionsJsonCached() {
		try {
			LinkedHashMap<String, Object> options = new LinkedHashMap<>();
			options.put("math_inline_delimiters", new String[] {"$", "$"});
			options.put("rm_spaces", true);
			options.put("formats", new String[] {"text", "latex_styled"});
			return objectMapper.writeValueAsString(options);
		} catch (Exception e) {
			throw MathpixOcrException.optionsJsonBuildFailed(e);
		}
	}

	private OcrResult parse(String response) {
		try {
			JsonNode root = objectMapper.readTree(response == null ? "{}" : response);
			String text = root.path(JSON_TEXT).asText(null);
			String latex = root.path(JSON_LATEX_STYLED).asText(null);

			if (text == null || text.isBlank()) {
				throw MathpixOcrException.emptyResponseText();
			}

			return new OcrResult(text, latex, root.toString());
		} catch (MathpixOcrException e) {
			throw e;
		} catch (Exception e) {
			throw MathpixOcrException.responseParseFailed(e);
		}
	}
}
