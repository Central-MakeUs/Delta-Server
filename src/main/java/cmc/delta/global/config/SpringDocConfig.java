package cmc.delta.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringDocConfig {

	@Bean
	public OpenAPI openAPI() {
		return new OpenAPI()
				.components(new Components()
						.addSchemas("ApiResponse", apiResponseSchema())
				);
	}

	private Schema<?> apiResponseSchema() {
		Schema<?> schema = new Schema<>();
		schema.setType("object");

		schema.addProperty("status",
				new IntegerSchema()
						.description("HTTP status code (e.g., 200, 400, 500)")
		);

		schema.addProperty("code",
				new StringSchema()
						.description("Application-level code (e.g., AUTH_001, REQ_001)")
		);

		schema.addProperty("data",
				new Schema<>()
						.type("object")
						.nullable(true)
						.description("Response payload (nullable)")
		);

		schema.addProperty("message",
				new StringSchema()
						.description("Human-readable message")
		);

		return schema;
	}
}
