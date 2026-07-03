package cmc.delta.domain.report.application.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 리포트 집계/서술 결과를 엔티티 JSON 컬럼에 저장·복원한다.
 */
@Component
@RequiredArgsConstructor
public class ReportPayloadCodec {

	private final ObjectMapper objectMapper;

	public String write(Object payload) {
		try {
			return objectMapper.writeValueAsString(payload);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("리포트 페이로드 직렬화 실패", e);
		}
	}

	public <T> T read(String json, Class<T> type) {
		if (json == null) {
			return null;
		}
		try {
			return objectMapper.readValue(json, type);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("리포트 페이로드 역직렬화 실패", e);
		}
	}
}
