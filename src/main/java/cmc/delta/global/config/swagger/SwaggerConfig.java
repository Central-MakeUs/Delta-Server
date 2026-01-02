package cmc.delta.global.config.swagger;

import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

  private SecurityScheme createBearerAuthScheme() {
    return new SecurityScheme()
        .type(SecurityScheme.Type.HTTP)
        .bearerFormat("JWT")
        .scheme("bearer");
  }

  private OpenApiCustomizer createOpenApiCustomizer(String title, String version) {
    return openApi -> {
      openApi.info(new Info().title(title).version(version));
      openApi.addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
      openApi.schemaRequirement("bearerAuth", createBearerAuthScheme());
    };
  }

  @Bean
  public GroupedOpenApi allApi(ApiErrorCodeOperationCustomizer apiErrorCodeOperationCustomizer) {
    return GroupedOpenApi.builder()
        .group("all")
        .pathsToMatch("/**")
        .displayName("All API")
        .addOpenApiCustomizer(createOpenApiCustomizer("모든 API", "v0.4"))
        .addOperationCustomizer(apiErrorCodeOperationCustomizer)
        .build();
  }
}
