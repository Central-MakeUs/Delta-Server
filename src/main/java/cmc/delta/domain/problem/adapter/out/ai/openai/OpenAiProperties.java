package cmc.delta.domain.problem.adapter.out.ai.openai;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "openai")
record OpenAiProperties(
	String baseUrl,
	String apiKey,
	String model,
	String solveModel) {
	public OpenAiProperties {
		if (baseUrl == null || baseUrl.isBlank()) {
			baseUrl = "https://api.openai.com";
		}
		if (model == null || model.isBlank()) {
			model = "gpt-4o-mini";
		}
		if (solveModel == null || solveModel.isBlank()) {
			solveModel = model;
		}
	}

	boolean isConfigured() {
		return apiKey != null && !apiKey.isBlank();
	}
}
