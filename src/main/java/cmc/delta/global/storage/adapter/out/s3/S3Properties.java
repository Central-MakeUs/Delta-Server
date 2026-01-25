package cmc.delta.global.storage.adapter.out.s3;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "storage.s3")
public record S3Properties(
	@NotBlank
	String bucket,
	@NotBlank
	String region,
	@NotBlank
	String keyPrefix,
	@Min(60)
	int presignGetTtlSeconds,
	@Min(1)
	long maxUploadBytes,
	@NotBlank
	String accessKey,
	@NotBlank
	String secretKey) {
}
