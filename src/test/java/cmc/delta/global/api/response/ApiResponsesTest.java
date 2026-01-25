package cmc.delta.global.api.response;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ApiResponsesTest {

	@Test
	@DisplayName("success: status가 200이면 OK 코드/메시지로 응답")
	void success_when200_thenOkCode() {
		// given
		String data = "ok";

		// when
		ApiResponse<String> resp = ApiResponses.success(200, data);

		// then
		assertThat(resp.status()).isEqualTo(200);
		assertThat(resp.code()).isEqualTo("S_200");
		assertThat(resp.message()).isEqualTo("요청에 성공했습니다.");
		assertThat(resp.data()).isEqualTo("ok");
	}

	@Test
	@DisplayName("success: status가 201이면 CREATED 코드/메시지로 응답")
	void success_when201_thenCreatedCode() {
		// given
		Integer data = 1;

		// when
		ApiResponse<Integer> resp = ApiResponses.success(201, data);

		// then
		assertThat(resp.status()).isEqualTo(201);
		assertThat(resp.code()).isEqualTo("S_201");
		assertThat(resp.message()).isEqualTo("생성에 성공했습니다.");
		assertThat(resp.data()).isEqualTo(1);
	}

	@Test
	@DisplayName("success: data 없는 success는 null data로 응답")
	void success_whenNoData_thenNullData() {
		// when
		ApiResponse<Void> resp = ApiResponses.success(200);

		// then
		assertThat(resp.data()).isNull();
		assertThat(resp.code()).isEqualTo("S_200");
	}

	@Test
	@DisplayName("fail: status/code/message/data를 그대로 세팅")
	void fail_whenCalled_thenKeepsFields() {
		// when
		ApiResponse<Object> resp = ApiResponses.fail(400, "REQ_001", "x", "bad");

		// then
		assertThat(resp.status()).isEqualTo(400);
		assertThat(resp.code()).isEqualTo("REQ_001");
		assertThat(resp.message()).isEqualTo("bad");
		assertThat(resp.data()).isEqualTo("x");
	}

	@Test
	@DisplayName("SuccessCode.fromStatus: 201/202만 특별 처리하고 나머지는 OK")
	void successCode_fromStatus_mapping() {
		assertThat(SuccessCode.fromStatus(201)).isEqualTo(SuccessCode.CREATED);
		assertThat(SuccessCode.fromStatus(202)).isEqualTo(SuccessCode.ACCEPTED);
		assertThat(SuccessCode.fromStatus(204)).isEqualTo(SuccessCode.OK);
	}
}
