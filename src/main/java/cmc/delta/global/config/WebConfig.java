package cmc.delta.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

	@Override
	public void addCorsMappings(CorsRegistry registry) {

		registry
			.addMapping("/**")
			.allowedOrigins("http://localhost:3000", "http://localhost:5173", "https://semo-xi.duckdns.org", "https://delta-semo.figma.site", "https://fe-dashboard-1kp.pages.dev")
			.allowedMethods("*")
			.allowedHeaders("*")
			.allowCredentials(true);
	}
}
