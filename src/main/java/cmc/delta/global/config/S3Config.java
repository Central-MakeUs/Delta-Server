package cmc.delta.global.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import cmc.delta.global.storage.adapter.out.s3.S3Properties;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
@EnableConfigurationProperties(S3Properties.class)
public class S3Config {

	@Bean
	S3Client s3Client(S3Properties properties) {
		return S3Client.builder()
			.region(Region.of(properties.region()))
			.credentialsProvider(credentialsProvider(properties))
			.build();
	}

	@Bean
	S3Presigner s3Presigner(S3Properties properties) {
		return S3Presigner.builder()
			.region(Region.of(properties.region()))
			.credentialsProvider(credentialsProvider(properties))
			.build();
	}

	private StaticCredentialsProvider credentialsProvider(S3Properties properties) {
		AwsBasicCredentials creds = AwsBasicCredentials.create(properties.accessKey(), properties.secretKey());
		return StaticCredentialsProvider.create(creds);
	}
}
