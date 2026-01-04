package cmc.delta.global.config.swagger;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

	private static final String SECURITY_SCHEME_NAME = "bearerAuth";

	private static final String BEARER_SCHEME = "bearer";
	private static final String BEARER_FORMAT_JWT = "JWT";

	private static final String GROUP_ALL = "all";
	private static final String DISPLAY_NAME_ALL = "All API";
	private static final String PATHS_MATCH_ALL = "/**";

	private static final String API_TITLE_ALL = "모든 API";
	private static final String API_VERSION = "v0.4";

	private SecurityScheme createBearerAuthScheme() {
		return new SecurityScheme()
			.type(SecurityScheme.Type.HTTP)
			.bearerFormat(BEARER_FORMAT_JWT)
			.scheme(BEARER_SCHEME);
	}

	private OpenApiCustomizer createOpenApiCustomizer(String title, String version) {
		return openApi -> {
			openApi.info(new Info().title(title).version(version));

			openApi.addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));

			Components components = openApi.getComponents();
			if (components == null) {
				components = new Components();
				openApi.setComponents(components);
			}
			components.addSecuritySchemes(SECURITY_SCHEME_NAME, createBearerAuthScheme());
		};
	}

	@Bean
	public GroupedOpenApi allApi(ApiErrorCodeOperationCustomizer apiErrorCodeOperationCustomizer) {
		return GroupedOpenApi.builder()
			.group(GROUP_ALL)
			.pathsToMatch(PATHS_MATCH_ALL)
			.displayName(DISPLAY_NAME_ALL)
			.addOpenApiCustomizer(createOpenApiCustomizer(API_TITLE_ALL, API_VERSION))
			.addOperationCustomizer(apiErrorCodeOperationCustomizer)
			.build();
	}
}
