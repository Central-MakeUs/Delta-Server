package cmc.delta.global.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class WebhookConfig {

	@Value("${discord.webhook.bot_toekn_url}")
	private String discordBotTokenUrl;
	private final RestTemplate restTemplate;

	public void sendDiscordNotification(Long userId) {
		String message = "Semo에 " + userId + "번째 유저가 가입했습니다!🎉";

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		Map<String, String> body = new HashMap<>();
		body.put("content", message);

		HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(body, headers);
		restTemplate.postForEntity(discordBotTokenUrl, requestEntity, String.class);
	}
}
