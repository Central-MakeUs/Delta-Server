package cmc.delta.global.config;

import cmc.delta.domain.problem.adapter.out.ocr.mathpix.MathpixProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(MathpixProperties.class)
class MathpixConfig {

	@Bean
	RestClient mathpixRestClient() {
		return RestClient.builder().build();
	}
}
