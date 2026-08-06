package cmc.delta.domain.version.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AppVersionPolicyInitializer implements ApplicationRunner {

	private final AppVersionQueryService appVersionQueryService;

	@Override
	public void run(ApplicationArguments args) {
		appVersionQueryService.initializeDefaultPolicy();
	}
}
