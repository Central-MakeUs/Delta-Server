package cmc.delta.domain.problem.adapter.out.ocr.mathpix;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mathpix")
public record MathpixProperties(
	String baseUrl,
	String appId,
	String appKey) {
}
