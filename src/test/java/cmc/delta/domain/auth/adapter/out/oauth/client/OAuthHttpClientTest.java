package cmc.delta.domain.auth.adapter.out.oauth.client;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

class OAuthHttpClientTest {

	@Test
	@DisplayName("postForm: 2xx면 body를 반환")
	void postForm_ok_returnsBody() {
		RestTemplate restTemplate = mock(RestTemplate.class);
		OAuthClientExceptionMapper exceptionMapper = mock(OAuthClientExceptionMapper.class);
		OAuthHttpClient client = new OAuthHttpClient(restTemplate, exceptionMapper);

		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("k", "v");

		when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(String.class)))
			.thenReturn(ResponseEntity.ok("ok"));

		String out = client.postForm("kakao", "token", "http://example", form, String.class);
		assertThat(out).isEqualTo("ok");
	}

	@Test
	@DisplayName("postForm: HttpStatusCodeException이면 exceptionMapper 결과를 throw")
	void postForm_whenHttpStatusException_thenThrowsMapped() {
		RestTemplate restTemplate = mock(RestTemplate.class);
		OAuthClientExceptionMapper exceptionMapper = mock(OAuthClientExceptionMapper.class);
		OAuthHttpClient client = new OAuthHttpClient(restTemplate, exceptionMapper);

		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();

		HttpClientErrorException httpEx = HttpClientErrorException.create(
			HttpStatus.UNAUTHORIZED,
			"unauth",
			HttpHeaders.EMPTY,
			new byte[0],
			StandardCharsets.UTF_8
		);
		when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(String.class)))
			.thenThrow(httpEx);
		RuntimeException mapped = new RuntimeException("mapped");
		when(exceptionMapper.mapHttpStatus("kakao", "token", httpEx)).thenReturn(mapped);

		Throwable thrown = catchThrowable(() -> client.postForm("kakao", "token", "http://example", form, String.class));
		assertThat(thrown).isSameAs(mapped);
	}

	@Test
	@DisplayName("get: ResourceAccessException이면 exceptionMapper 결과를 throw")
	void get_whenTimeout_thenThrowsMapped() {
		RestTemplate restTemplate = mock(RestTemplate.class);
		OAuthClientExceptionMapper exceptionMapper = mock(OAuthClientExceptionMapper.class);
		OAuthHttpClient client = new OAuthHttpClient(restTemplate, exceptionMapper);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		ResourceAccessException timeout = new ResourceAccessException("timeout");
		when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(String.class))).thenThrow(timeout);
		RuntimeException mapped = new RuntimeException("mapped");
		when(exceptionMapper.mapTimeout("kakao", "profile", timeout)).thenReturn(mapped);

		Throwable thrown = catchThrowable(() -> client.get("kakao", "profile", "http://example", headers, String.class));
		assertThat(thrown).isSameAs(mapped);
	}
}
