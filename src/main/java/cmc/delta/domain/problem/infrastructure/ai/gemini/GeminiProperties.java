package cmc.delta.domain.problem.infrastructure.ai.gemini;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gemini")
record GeminiProperties(
	String baseUrl,
	String apiKey,
	String model
) {
	public GeminiProperties {
		if (baseUrl == null || baseUrl.isBlank()) baseUrl = "https://generativelanguage.googleapis.com";
		if (model == null || model.isBlank()) model = "gemini-2.5-flash-lite";
	}
}