package cmc.delta.domain.problem.adapter.out.ocr.mathpix;

import cmc.delta.domain.problem.application.port.out.ocr.OcrClient;
import cmc.delta.domain.problem.application.port.out.ocr.dto.OcrResult;
import cmc.delta.domain.problem.application.port.out.ocr.exception.OcrTextNotDetectedException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.LinkedHashMap;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
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
	public OcrResult recognize(InputStream imageStream, long contentLength, String filename) {
		try {
			MultiValueMap<String, Object> form = buildForm(imageStream, contentLength, filename);

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
		} catch (OcrTextNotDetectedException e) {
			throw e;
		} catch (MathpixOcrException e) {
			throw e;
		} catch (Exception e) {
			throw MathpixOcrException.responseParseFailed(e);
		}
	}

	private MultiValueMap<String, Object> buildForm(InputStream imageStream, long contentLength, String filename) {
		MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
		SizedInputStreamResource resource = new SizedInputStreamResource(imageStream, contentLength, filename);
		HttpHeaders partHeaders = new HttpHeaders();
		partHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
		partHeaders.setContentDispositionFormData(FORM_FILE, filename);
		form.add(FORM_FILE, new HttpEntity<>(resource, partHeaders));
		form.add(FORM_OPTIONS_JSON, optionsJson);
		return form;
	}

	private static final class SizedInputStreamResource extends InputStreamResource {

		private final long contentLength;
		private final String filename;

		private SizedInputStreamResource(InputStream inputStream, long contentLength, String filename) {
			super(inputStream);
			this.contentLength = contentLength;
			this.filename = filename;
		}

		@Override
		public long contentLength() {
			return contentLength;
		}

		@Override
		public String getFilename() {
			return filename;
		}
	}

	private String buildOptionsJsonCached() {
		try {
			LinkedHashMap<String, Object> options = new LinkedHashMap<>();
			options.put("math_inline_delimiters", new String[] {"$", "$"});
			options.put("rm_spaces", true);
			options.put("include_line_data", true);
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
				throw new OcrTextNotDetectedException();
			}

			return new OcrResult(text, latex, root.toString());
		} catch (OcrTextNotDetectedException e) {
			throw e;
		} catch (MathpixOcrException e) {
			throw e;
		} catch (Exception e) {
			throw MathpixOcrException.responseParseFailed(e);
		}
	}
}
