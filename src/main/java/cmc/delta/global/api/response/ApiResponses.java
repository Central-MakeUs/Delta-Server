package cmc.delta.global.api.response;

public final class ApiResponses {

	private ApiResponses() {}

	public static <T> ApiResponse<T> success(int status, T data) {
		SuccessCode sc = SuccessCode.fromStatus(status);
		return new ApiResponse<>(sc.status(), sc.code(), data, sc.message());
	}

	public static ApiResponse<Void> success(int status) {
		SuccessCode sc = SuccessCode.fromStatus(status);
		return new ApiResponse<>(sc.status(), sc.code(), null, sc.message());
	}

	public static ApiResponse<Object> fail(int status, String code, Object data, String message) {
		return new ApiResponse<>(status, code, data, message);
	}
}
