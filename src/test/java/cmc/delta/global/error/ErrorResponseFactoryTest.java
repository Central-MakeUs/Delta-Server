package cmc.delta.global.error;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import cmc.delta.global.api.response.ApiResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

class ErrorResponseFactoryTest {

	private final ErrorResponseFactory factory = new ErrorResponseFactory();

	@Test
	@DisplayName("validationError: FieldError message가 blank면 invalid로 fallback")
	void validationError_whenBlankFieldErrorMessage_thenFallbackInvalid() throws Exception {
		// given
		DummyRequest target = new DummyRequest();
		BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(target, "req");
		bindingResult.addError(new FieldError("req", "name", ""));

		Method method = DummyController.class.getDeclaredMethod("call", DummyRequest.class);
		MethodParameter param = new MethodParameter(method, 0);
		MethodArgumentNotValidException ex = new MethodArgumentNotValidException(param, bindingResult);

		// when
		ApiResponse<Object> resp = factory.validationError(ErrorCode.INVALID_REQUEST, ex);

		// then
		assertThat(resp.status()).isEqualTo(400);
		assertThat(resp.code()).isEqualTo(ErrorCode.INVALID_REQUEST.code());
		assertThat(resp.message()).isEqualTo(ErrorCode.INVALID_REQUEST.defaultMessage());

		assertThat(resp.data()).isInstanceOf(Map.class);
		Map<?, ?> data = (Map<?, ?>) resp.data();
		assertThat(data.get("fieldErrors")).isInstanceOf(Map.class);
		Map<?, ?> fieldErrors = (Map<?, ?>) data.get("fieldErrors");
		assertThat(fieldErrors.get("name")).isEqualTo("invalid");
	}

	@Test
	@DisplayName("constraintViolationError: violations를 path->message map으로 만든다")
	void constraintViolationError_whenViolations_thenMapsToResponse() {
		// given
		ConstraintViolation<?> v = mock(ConstraintViolation.class);
		Path path = mock(Path.class);
		when(path.toString()).thenReturn("name");
		when(v.getPropertyPath()).thenReturn(path);
		when(v.getMessage()).thenReturn("must not be blank");

		Set<ConstraintViolation<?>> violations = Set.of(v);
		ConstraintViolationException ex = new ConstraintViolationException(violations);

		// when
		ApiResponse<Object> resp = factory.constraintViolationError(ErrorCode.INVALID_REQUEST, ex);

		// then
		assertThat(resp.data()).isInstanceOf(Map.class);
		Map<?, ?> data = (Map<?, ?>) resp.data();
		assertThat(data.get("violations")).isInstanceOf(Map.class);
		Map<?, ?> mapped = (Map<?, ?>) data.get("violations");
		assertThat(mapped.get("name")).isEqualTo("must not be blank");
	}

	private static class DummyRequest {
		private String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	private static class DummyController {
		@SuppressWarnings("unused")
		public void call(DummyRequest req) {
		}
	}
}
