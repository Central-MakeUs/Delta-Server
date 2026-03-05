package cmc.delta.domain.problem.adapter.out.ai.gemini;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.mock;

import cmc.delta.domain.problem.application.port.out.ai.dto.ProblemAiSolveResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

class GeminiProblemSolveAiClientTest {

	private final GeminiProblemSolveAiClient client = new GeminiProblemSolveAiClient(
		new GeminiProperties("https://generativelanguage.googleapis.com", "test-key", "gemini-2.5-flash-lite",
			"gemini-2.5-flash-lite"),
		new ObjectMapper(),
		mock(RestClient.class));

	@Test
	@DisplayName("정상 JSON 응답은 풀이를 파싱한다")
	void parseOrFallback_validJson_parsesSolveResult() {
		String responseText = """
			{"solution_latex":"f(x)=x^2","solution_text":"간단 풀이","final_answer":"0"}
			""";

		ProblemAiSolveResult result = invokeParseOrFallback(responseText);

		assertThat(result.solutionLatex()).isEqualTo("f(x)=x^2");
		assertThat(result.solutionText()).isEqualTo("간단 풀이");
	}

	@Test
	@DisplayName("닫히지 않은 JSON 객체도 핵심 필드가 있으면 보정 후 파싱한다")
	void parseOrFallback_truncatedJsonObject_repairsAndParses() {
		String truncated = """
			{"solution_latex":"A\\nB","solution_text":"C\\\"D","final_answer":"1"
			""";

		ProblemAiSolveResult result = invokeParseOrFallback(truncated);

		assertThat(result.solutionLatex()).isEqualTo("A\nB");
		assertThat(result.solutionText()).isEqualTo("C\"D");
	}

	@Test
	@DisplayName("과도한 반복 풀이 텍스트는 비정상 응답으로 거부한다")
	void parseOrFallback_repeatedLines_rejectsDegenerateText() {
		String repeatedLine = "따라서 P는 y=x^2 위의 점이어야 한다.";
		StringBuilder builder = new StringBuilder();
		for (int index = 0; index < 6; index++) {
			if (builder.length() > 0) {
				builder.append("\\n");
			}
			builder.append(repeatedLine);
		}
		String payload = "{\"solution_latex\":\"x=1\",\"solution_text\":\""
			+ builder + "\",\"final_answer\":\"정답: 1\"}";

		GeminiAiException exception = catchThrowableOfType(
			() -> invokeParseOrFallback(payload),
			GeminiAiException.class);

		assertThat(exception).isNotNull();
		assertThat(exception.getMessage()).contains(GeminiAiException.REASON_RESPONSE_PARSE_FAILED);
	}

	@Test
	@DisplayName("닫히지 않은 필드 값만 남은 malformed 응답도 보정 후 최소 필드를 추출한다")
	void parseOrFallback_truncatedTailOnly_repairsAndExtracts() {
		String malformed = """
			{"solution_latex":"주어진 함수는 f(x)=ax^2 이고, 교점 조건을 사용한다
			""";

		ProblemAiSolveResult result = invokeParseOrFallback(malformed);

		assertThat(result.solutionLatex()).contains("주어진 함수는 f(x)=ax^2");
		assertThat(result.solutionText()).contains("주어진 함수는 f(x)=ax^2");
	}

	private ProblemAiSolveResult invokeParseOrFallback(String modelText) {
		Object result = ReflectionTestUtils.invokeMethod(client, "parseOrFallback", modelText);
		assertThat(result).isInstanceOf(ProblemAiSolveResult.class);
		return (ProblemAiSolveResult)result;
	}
}
