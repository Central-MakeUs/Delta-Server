package cmc.delta.domain.stats.adapter.out.discord;

import java.io.IOException;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import cmc.delta.domain.stats.application.dto.DailyStatsReport;
import cmc.delta.domain.stats.application.dto.PeriodStats;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class DailyStatsDiscordNotifier {

	private static final DateTimeFormatter TIMESTAMP_FORMATTER =
		DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 HH:mm");
	private static final DateTimeFormatter DATE_FORMATTER =
		DateTimeFormatter.ofPattern("MM월 dd일");

	private static final String SWINGS_IMAGE_RESOURCE = "swings.png";
	private static final String DIVIDER = "━━━━━━━━━━━━━━━━━━━━━━";

	private static final String DISCORD_FIELD_PAYLOAD = "payload_json";
	private static final String DISCORD_FIELD_FILE = "file";
	private static final String DISCORD_FIELD_CONTENT = "content";

	private static final String PERIOD_LABEL_TODAY = "오늘 하루";
	private static final String PERIOD_LABEL_LAST_3_DAYS = "최근 3일";
	private static final String PERIOD_LABEL_LAST_7_DAYS = "최근 7일";

	private static final String HEADER_FORMAT =
		"# 잘 들으세요. %s 세모 리포트에요.\n"
		+ "근거 없이 자신을 믿으세요. 근데 숫자는 거짓말 안 해요.\n";

	private static final String FOOTER =
		"끝까지 가는 사람이 무조건 이겨요. 올해는 우리 꺼.";

	private static final String PERIOD_HEADER_FORMAT = "**%s** `%s ~ %s` — %s";

	// 오늘 말투 — 담담하게
	private static final String TODAY_NEW_USERS_FORMAT =
		"> 신규 가입자 **%d명**. 환영은 니들이 해요. 난 바빠요.";
	private static final String TODAY_SCANS_FORMAT =
		"> 문제 스캔 **%d번**. 찍는다고 실력 느는 거 아닌 거 다들 알죠?";
	private static final String TODAY_WRONG_ANSWER_CARDS_FORMAT =
		"> 오답카드 **%d개**. 틀린 거 인정하는 건 좋아요. 근데 외워요.";
	private static final String TODAY_AI_SOLUTION_FORMAT =
		"> AI 풀이 **%d번**. 존나 웃기네. 귀엽네. 근데 스스로도 좀 생각해요.";

	// 3일 말투 — 좀 더 냉소적으로
	private static final String LAST_3_DAYS_NEW_USERS_FORMAT =
		"> 신규 가입자 **%d명**. 근데 얼마나 남을지는 두고 봐야죠.";
	private static final String LAST_3_DAYS_SCANS_FORMAT =
		"> 문제 스캔 **%d번**. 3일 동안 이게 다예요? 더 할 수 있잖아요.";
	private static final String LAST_3_DAYS_WRONG_ANSWER_CARDS_FORMAT =
		"> 오답카드 **%d개**. 만들고 안 보면 의미 없어요. 제가 다 알아요.";
	private static final String LAST_3_DAYS_AI_SOLUTION_FORMAT =
		"> AI 풀이 **%d번**. 선빵은 항상 실력이에요. AI가 쳐줄 것 같아요?";

	// 7일 말투 — 총평하듯 단호하게
	private static final String LAST_7_DAYS_NEW_USERS_FORMAT =
		"> 신규 가입자 **%d명**. 일주일 기준이에요. 성장하고 있어요, 아직은.";
	private static final String LAST_7_DAYS_SCANS_FORMAT =
		"> 문제 스캔 **%d번**. 일주일 합산이에요. 내가 할 수 있다고 했죠?";
	private static final String LAST_7_DAYS_WRONG_ANSWER_CARDS_FORMAT =
		"> 오답카드 **%d개**. 이 숫자가 실력이에요. 변명은 필요 없어요.";
	private static final String LAST_7_DAYS_AI_SOLUTION_FORMAT =
		"> AI 풀이 **%d번**. 7일 동안 이만큼 물어봤어요. 내가 또 증명해내는 거 봤어요?";

	@Value("${discord.webhook.bot_token_stats}")
	private String discordWebhookUrl;

	private final RestTemplate restTemplate;
	private final ObjectMapper objectMapper;

	public void send(DailyStatsReport report) {
		try {
			postWithImage(buildMessage(report));
			log.info("일일 통계 Discord 전송 완료");
		} catch (Exception e) {
			log.error("Discord 일일 통계 전송 실패", e);
		}
	}

	private String buildMessage(DailyStatsReport report) {
		String timestamp = report.generatedAt().format(TIMESTAMP_FORMATTER);

		return HEADER_FORMAT.formatted(timestamp)
			+ "\n" + DIVIDER + "\n"
			+ periodHeader(PERIOD_LABEL_TODAY, report.today(), "증명해낸 거 봤어요?") + "\n"
			+ formatToday(report.today())
			+ "\n\n" + DIVIDER + "\n"
			+ periodHeader(PERIOD_LABEL_LAST_3_DAYS, report.last3Days(), "3일 동안 뭐 했어요?") + "\n"
			+ formatLast3Days(report.last3Days())
			+ "\n\n" + DIVIDER + "\n"
			+ periodHeader(PERIOD_LABEL_LAST_7_DAYS, report.last7Days(), "일주일 총평이에요.") + "\n"
			+ formatLast7Days(report.last7Days())
			+ "\n\n" + DIVIDER + "\n"
			+ FOOTER;
	}

	private String periodHeader(String label, PeriodStats stats, String comment) {
		String from = stats.from().format(DATE_FORMATTER);
		String to = stats.to().format(DATE_FORMATTER);
		return PERIOD_HEADER_FORMAT.formatted(label, from, to, comment);
	}

	private String formatToday(PeriodStats stats) {
		return TODAY_NEW_USERS_FORMAT.formatted(stats.newUsers()) + "\n"
			+ TODAY_SCANS_FORMAT.formatted(stats.scans()) + "\n"
			+ TODAY_WRONG_ANSWER_CARDS_FORMAT.formatted(stats.wrongAnswerCards()) + "\n"
			+ TODAY_AI_SOLUTION_FORMAT.formatted(stats.aiSolutionAttempts());
	}

	private String formatLast3Days(PeriodStats stats) {
		return LAST_3_DAYS_NEW_USERS_FORMAT.formatted(stats.newUsers()) + "\n"
			+ LAST_3_DAYS_SCANS_FORMAT.formatted(stats.scans()) + "\n"
			+ LAST_3_DAYS_WRONG_ANSWER_CARDS_FORMAT.formatted(stats.wrongAnswerCards()) + "\n"
			+ LAST_3_DAYS_AI_SOLUTION_FORMAT.formatted(stats.aiSolutionAttempts());
	}

	private String formatLast7Days(PeriodStats stats) {
		return LAST_7_DAYS_NEW_USERS_FORMAT.formatted(stats.newUsers()) + "\n"
			+ LAST_7_DAYS_SCANS_FORMAT.formatted(stats.scans()) + "\n"
			+ LAST_7_DAYS_WRONG_ANSWER_CARDS_FORMAT.formatted(stats.wrongAnswerCards()) + "\n"
			+ LAST_7_DAYS_AI_SOLUTION_FORMAT.formatted(stats.aiSolutionAttempts());
	}

	private void postWithImage(String message) throws IOException {
		ClassPathResource imageResource = new ClassPathResource(SWINGS_IMAGE_RESOURCE);

		try (InputStream imageStream = imageResource.getInputStream()) {
			byte[] imageBytes = imageStream.readAllBytes();

			String payload = objectMapper.writeValueAsString(Map.of(DISCORD_FIELD_CONTENT, message));

			HttpHeaders multipartHeaders = new HttpHeaders();
			multipartHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);

			MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
			body.add(DISCORD_FIELD_PAYLOAD, new HttpEntity<>(payload,
				headersWithContentType(MediaType.APPLICATION_JSON)));
			body.add(DISCORD_FIELD_FILE, new HttpEntity<>(imageBytes,
				headersWithFilename(SWINGS_IMAGE_RESOURCE, MediaType.IMAGE_PNG)));

			restTemplate.postForEntity(discordWebhookUrl,
				new HttpEntity<>(body, multipartHeaders), String.class);
		}
	}

	private HttpHeaders headersWithContentType(MediaType mediaType) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(mediaType);
		return headers;
	}

	private HttpHeaders headersWithFilename(String filename, MediaType mediaType) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(mediaType);
		headers.setContentDispositionFormData("file", filename);
		return headers;
	}
}
