package cmc.delta;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

import cmc.delta.domain.problem.application.worker.properties.AiWorkerProperties;
import cmc.delta.domain.problem.application.worker.properties.OcrWorkerProperties;

@EnableScheduling
@EnableJpaAuditing
@EnableConfigurationProperties({OcrWorkerProperties.class, AiWorkerProperties.class})
@SpringBootApplication
public class DeltaApplication {

	public static void main(String[] args) {
		SpringApplication.run(DeltaApplication.class, args);
	}
}
