package cmc.delta.domain.auth.adapter.out.oauth.client;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

/**
 * 외부 OAuth 호출 공통 클라이언트.
 *
 * - HTTP 호출(POST form, GET)
 * - 접근 로그(provider/operation/status/duration)
 * - 예외를 OAuthClientExceptionMapper로 위임해 "정책 기반" 변환
 *
 * 로깅 정책:
 * - 성공: provider/operation/status/duration 만 DEBUG 로 기록 (응답 body 절대 미기록).
 * - 실패: provider 응답 body 를 WARN 으로 기록하되, SensitiveBodyMasker 로 access_token/refresh_token/id_token/code/client_secret 마스킹.
 */
@Slf4j
@RequiredArgsConstructor
public class OAuthHttpClient {

	private final RestTemplate restTemplate;
	private final OAuthClientExceptionMapper exceptionMapper;

	public <T> T postForm(
		String providerName,
		String operation,
		String url,
		MultiValueMap<String, String> form,
		Class<T> responseType) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));

		HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);
		long start = System.nanoTime();

		try {
			ResponseEntity<T> response = restTemplate.exchange(url, HttpMethod.POST, request, responseType);
			logOk(providerName, operation, response.getStatusCode(), start);
			return response.getBody();

		} catch (HttpStatusCodeException e) {
			logFail(providerName, operation, e.getStatusCode(), start, e.getResponseBodyAsString());
			throw exceptionMapper.mapHttpStatus(providerName, operation, e);

		} catch (ResourceAccessException e) {
			logFail(providerName, operation, null, start, null);
			throw exceptionMapper.mapTimeout(providerName, operation, e);
		}
	}

	public <T> T get(
		String providerName,
		String operation,
		String url,
		HttpHeaders headers,
		Class<T> responseType) {
		headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));

		HttpEntity<Void> request = new HttpEntity<>(headers);
		long start = System.nanoTime();

		try {
			ResponseEntity<T> response = restTemplate.exchange(url, HttpMethod.GET, request, responseType);
			logOk(providerName, operation, response.getStatusCode(), start);
			return response.getBody();

		} catch (HttpStatusCodeException e) {
			logFail(providerName, operation, e.getStatusCode(), start, e.getResponseBodyAsString());
			throw exceptionMapper.mapHttpStatus(providerName, operation, e);

		} catch (ResourceAccessException e) {
			logFail(providerName, operation, null, start, null);
			throw exceptionMapper.mapTimeout(providerName, operation, e);
		}
	}

	private void logOk(String provider, String operation, HttpStatusCode status, long startNano) {
		long durationMs = Duration.ofNanos(System.nanoTime() - startNano).toMillis();
		log.debug("oauth_external_ok provider={} operation={} status={} durationMs={}",
			provider, operation, status.value(), durationMs);
	}

	private void logFail(String provider, String operation, HttpStatusCode status, long startNano,
		String responseBody) {
		long durationMs = Duration.ofNanos(System.nanoTime() - startNano).toMillis();
		String statusValue = (status == null) ? "NA" : String.valueOf(status.value());
		// 실패 디버깅 목적으로 body 를 남기되, access_token/code 등 민감 필드는 마스킹한다.
		String maskedBody = SensitiveBodyMasker.mask(responseBody);
		log.warn("oauth_external_fail provider={} operation={} status={} durationMs={} body={}",
			provider, operation, statusValue, durationMs, maskedBody);
	}
}
