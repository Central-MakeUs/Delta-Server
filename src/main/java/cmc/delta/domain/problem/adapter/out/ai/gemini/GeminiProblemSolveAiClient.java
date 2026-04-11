package cmc.delta.domain.problem.adapter.out.ai.gemini;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import cmc.delta.domain.problem.adapter.out.ai.AiResponseParseUtils;
import cmc.delta.domain.problem.adapter.out.ai.SolvePromptTemplate;
import cmc.delta.domain.problem.application.port.out.ai.ProblemSolveAiClient;
import cmc.delta.domain.problem.application.port.out.ai.dto.ProblemAiSolvePrompt;
import cmc.delta.domain.problem.application.port.out.ai.dto.ProblemAiSolveResult;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@EnableConfigurationProperties(GeminiProperties.class)
public class GeminiProblemSolveAiClient implements ProblemSolveAiClient {

	private static final int LOG_TEXT_LIMIT = 1200;
	private static final int SOLVE_MAX_OUTPUT_TOKENS = 8192;
	private static final int SOLVE_THINKING_BUDGET = 0;
	private static final long NANOS_PER_MILLISECOND = 1_000_000L;

	private static final String PATH_GENERATE_CONTENT = "/v1beta/models/{model}:generateContent";
	private static final String QUERY_KEY = "key";

	private static final String FIELD_SOLUTION_LATEX = "solution_latex";
	private static final String FIELD_SOLUTION_TEXT = "solution_text";
	private static final String FIELD_FINAL_ANSWER = "final_answer";
	private static final Map<String, Object> RESPONSE_SCHEMA = GeminiSolveSchemaFactory.responseSchema();

	private final GeminiProperties props;
	private final ObjectMapper objectMapper;
	private final RestClient geminiRestClient;
	private final GeminiSolveResponseExtractor responseExtractor;
	private final GeminiSolveMalformedParser malformedParser;
	private final GeminiSolveDegenerateDetector degenerateDetector;
	private final GeminiSolveTextNormalizer textNormalizer;

	public GeminiProblemSolveAiClient(
		GeminiProperties props,
		ObjectMapper objectMapper,
		@Qualifier("geminiRestClient") RestClient geminiRestClient,
		GeminiSolveResponseExtractor responseExtractor,
		GeminiSolveMalformedParser malformedParser,
		GeminiSolveDegenerateDetector degenerateDetector,
		GeminiSolveTextNormalizer textNormalizer) {
		this.props = props;
		this.objectMapper = objectMapper;
		this.geminiRestClient = geminiRestClient;
		this.responseExtractor = responseExtractor;
		this.malformedParser = malformedParser;
		this.degenerateDetector = degenerateDetector;
		this.textNormalizer = textNormalizer;
	}

	@Override
	public ProblemAiSolveResult solveProblem(ProblemAiSolvePrompt prompt) {
		long startedAtNanos = System.nanoTime();
		try {
			String promptText = buildPromptText();
			Map<String, Object> requestBody = buildRequestBody(
				promptText,
				prompt.problemImageBytes(),
				prompt.problemImageMimeType());
			log.debug(
				"Gemini 풀이 요청 model={} imageBytes={} imageMimeType={} promptLength={}",
				props.solveModel(),
				prompt.problemImageBytes() == null ? 0 : prompt.problemImageBytes().length,
				prompt.problemImageMimeType(),
				promptText.length());

			String rawResponseJson = callApi(requestBody);
			log.debug(
				"Gemini 풀이 원본 응답 수신 model={} rawLength={} rawSnippet={}",
				props.solveModel(),
				rawResponseJson == null ? 0 : rawResponseJson.length(),
				abbreviate(rawResponseJson));

			ProblemAiSolveResult result = parseResponse(rawResponseJson);
			log.info("Gemini 풀이 완료 model={} durationMs={}", props.solveModel(), elapsedMillis(startedAtNanos));
			return result;
		} catch (RestClientResponseException e) {
			log.warn(
				"Gemini 풀이 HTTP 실패 model={} status={} durationMs={} responseBodySnippet={}",
				props.solveModel(),
				e.getRawStatusCode(),
				elapsedMillis(startedAtNanos),
				abbreviate(e.getResponseBodyAsString()));
			throw GeminiAiException.externalCallFailed(e);
		} catch (GeminiAiException e) {
			log.warn(
				"Gemini 풀이 실패 model={} durationMs={} message={}",
				props.solveModel(),
				elapsedMillis(startedAtNanos),
				e.getMessage(),
				e);
			throw e;
		} catch (Exception e) {
			log.warn(
				"Gemini 풀이 예외 model={} durationMs={} message={}",
				props.solveModel(),
				elapsedMillis(startedAtNanos),
				e.getMessage(),
				e);
			throw GeminiAiException.responseParseFailed(e);
		}
	}

	private String callApi(Map<String, Object> requestBody) {
		return geminiRestClient
			.post()
			.uri(uriBuilder -> uriBuilder
				.path(PATH_GENERATE_CONTENT)
				.queryParam(QUERY_KEY, props.apiKey())
				.build(props.solveModel()))
			.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
			.body(requestBody)
			.retrieve()
			.body(String.class);
	}

	private String buildPromptText() {
		try {
			return SolvePromptTemplate.render();
		} catch (Exception e) {
			throw GeminiAiException.promptBuildFailed(e);
		}
	}

	private Map<String, Object> buildRequestBody(String promptText, byte[] imageBytes, String imageMimeType) {
		if (imageBytes == null || imageBytes.length == 0) {
			throw GeminiAiException.promptBuildFailed(new IllegalArgumentException("Problem image bytes are empty"));
		}

		String safeMimeType = (imageMimeType == null || imageMimeType.isBlank()) ? "image/jpeg" : imageMimeType;
		String base64Image = Base64.getEncoder().encodeToString(imageBytes);

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("contents", List.of(Map.of(
			"role", "user",
			"parts", List.of(
				Map.of("inline_data", Map.of("mime_type", safeMimeType, "data", base64Image)),
				Map.of("text", promptText)))));

		Map<String, Object> generationConfig = new LinkedHashMap<>();
		generationConfig.put("temperature", 0);
		generationConfig.put("maxOutputTokens", SOLVE_MAX_OUTPUT_TOKENS);
		generationConfig.put("responseMimeType", "application/json");
		generationConfig.put("responseSchema", RESPONSE_SCHEMA);
		generationConfig.put("thinkingConfig", Map.of("thinkingBudget", SOLVE_THINKING_BUDGET));

		body.put("generationConfig", generationConfig);
		return body;
	}

	private ProblemAiSolveResult parseResponse(String rawResponseJson) {
		try {
			String modelText = responseExtractor.extractModelJsonText(rawResponseJson);
			return parseOrFallback(modelText);
		} catch (GeminiAiException e) {
			throw e;
		} catch (Exception e) {
			log.debug("Gemini 풀이 parseResponse 실패 rawSnippet={} reason={}", abbreviate(rawResponseJson), e.getMessage(), e);
			throw GeminiAiException.responseParseFailed(e);
		}
	}

	private ProblemAiSolveResult parseOrFallback(String modelText) {
		String normalizedModelText = AiResponseParseUtils.stripMarkdownCodeFence(modelText);
		String unwrappedModelText = responseExtractor.unwrapJsonTextNodeIfNeeded(normalizedModelText);
		String jsonPayload = responseExtractor.extractJsonObject(unwrappedModelText);
		if (jsonPayload == null) {
			jsonPayload = responseExtractor.repairTruncatedJsonObject(unwrappedModelText);
		}

		if (jsonPayload != null) {
			ProblemAiSolveResult parsed = tryParseJson(jsonPayload, unwrappedModelText);
			if (parsed != null) {
				return parsed;
			}
		} else {
			log.debug("Gemini 풀이 응답에서 JSON 객체를 찾지 못함. malformed fallback 시도 textLength={}",
				unwrappedModelText.length());
		}

		GeminiSolveMalformedParser.ParsedFields extracted = malformedParser.parse(unwrappedModelText);
		if (extracted != null) {
			log.debug(
				"Gemini 풀이 malformed structured text 추출 성공 latexLength={} textLength={}",
				extracted.solutionLatex() == null ? 0 : extracted.solutionLatex().length(),
				extracted.solutionText() == null ? 0 : extracted.solutionText().length());
			return finalizeResult(extracted.solutionLatex(), extracted.solutionText());
		}

		if (unwrappedModelText.isBlank()) {
			throw GeminiAiException.emptyText();
		}
		throw GeminiAiException.responseParseFailed(
			new IllegalArgumentException("Structured solve response parse failed"));
	}

	private ProblemAiSolveResult tryParseJson(String jsonPayload, String unwrappedModelText) {
		try {
			JsonNode root = objectMapper.readTree(jsonPayload);
			String solutionLatex = AiResponseParseUtils.readTextOrNull(root, FIELD_SOLUTION_LATEX);
			String solutionText = AiResponseParseUtils.readTextOrNull(root, FIELD_SOLUTION_TEXT);
			String finalAnswer = AiResponseParseUtils.readTextOrNull(root, FIELD_FINAL_ANSWER);
			if (solutionLatex != null || solutionText != null || finalAnswer != null) {
				return finalizeResult(solutionLatex, solutionText);
			}
			log.debug("Gemini 풀이 JSON 파싱 성공했지만 필수 필드 누락. malformed fallback 시도 textLength={}",
				unwrappedModelText.length());
		} catch (Exception parseException) {
			log.debug("Gemini 풀이 JSON 파싱 실패. fallback 사용 textLength={} reason={}",
				unwrappedModelText.length(),
				parseException.getMessage());
		}
		return null;
	}

	private ProblemAiSolveResult finalizeResult(String rawLatex, String rawPlainText) {
		String latex = textNormalizer.normalizeDisplayText(rawLatex);
		String plainText = textNormalizer.normalizeDisplayText(rawPlainText);
		if (plainText == null || plainText.isBlank()) {
			plainText = latex;
		}
		if (AiResponseParseUtils.shouldPreferLatexText(latex, plainText)) {
			plainText = latex;
		}
		if (degenerateDetector.isDegenerate(plainText)) {
			throw GeminiAiException.responseParseFailed(
				new IllegalArgumentException("Degenerate solve text rejected"));
		}
		return new ProblemAiSolveResult(latex, plainText);
	}

	private long elapsedMillis(long startedAtNanos) {
		return (System.nanoTime() - startedAtNanos) / NANOS_PER_MILLISECOND;
	}

	private String abbreviate(String text) {
		if (text == null || text.isBlank()) {
			return "";
		}
		String compact = text.replace("\n", "\\n").replace("\r", "\\r");
		if (compact.length() <= LOG_TEXT_LIMIT) {
			return compact;
		}
		return compact.substring(0, LOG_TEXT_LIMIT) + "...";
	}
}
