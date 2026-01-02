package cmc.delta.global.config.swagger;

import cmc.delta.global.error.ErrorCode;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;

import java.util.*;

@Component
public class ApiErrorCodeOperationCustomizer implements OperationCustomizer {

	private static final String APPLICATION_JSON = "application/json";
	private static final String API_RESPONSE_SCHEMA_REF = "#/components/schemas/ApiResponse";

	@Override
	public Operation customize(Operation operation, HandlerMethod handlerMethod) {

		ApiErrorCodeExample single = handlerMethod.getMethodAnnotation(ApiErrorCodeExample.class);
		ApiErrorCodeExamples multiple = handlerMethod.getMethodAnnotation(ApiErrorCodeExamples.class);

		List<ErrorCode> errorCodes = new ArrayList<>();

		if (single != null) {
			errorCodes.add(single.value());
		}
		if (multiple != null) {
			errorCodes.addAll(Arrays.asList(multiple.value()));
		}
		if (errorCodes.isEmpty()) {
			return operation;
		}

		ApiResponses responses = operation.getResponses();
		if (responses == null) {
			responses = new ApiResponses();
			operation.setResponses(responses);
		}

		for (ErrorCode errorCode : errorCodes) {
			String statusCode = String.valueOf(errorCode.status().value());

			ExampleHolder holder = createExampleHolder(errorCode);

			ApiResponse apiResponse = responses.computeIfAbsent(statusCode, code -> new ApiResponse());

			if (apiResponse.getDescription() == null || apiResponse.getDescription().isBlank()) {
				apiResponse.setDescription(errorCode.defaultMessage());
			}

			Content content = apiResponse.getContent();
			if (content == null) {
				content = new Content();
				apiResponse.setContent(content);
			}

			MediaType mediaType = content.get(APPLICATION_JSON);
			if (mediaType == null) {
				mediaType = new MediaType();
				mediaType.setSchema(new Schema<>().$ref(API_RESPONSE_SCHEMA_REF));
				content.addMediaType(APPLICATION_JSON, mediaType);
			}

			mediaType.addExamples(holder.getName(), holder.getHolder());
		}

		return operation;
	}

	private ExampleHolder createExampleHolder(ErrorCode errorCode) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("status", errorCode.status().value());
		body.put("code", errorCode.code());
		body.put("data", null);
		body.put("message", errorCode.defaultMessage());

		Example example = new Example();
		example.setSummary(errorCode.name());
		example.setDescription(errorCode.defaultMessage());
		example.setValue(body);

		return ExampleHolder.builder()
				.name(errorCode.name())
				.status(errorCode.status().value())
				.code(errorCode.code())
				.holder(example)
				.build();
	}
}
