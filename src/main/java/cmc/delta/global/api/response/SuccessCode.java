package cmc.delta.global.api.response;

public enum SuccessCode {
	OK(200, "S_200", "요청에 성공했습니다."),
	CREATED(201, "S_201", "생성에 성공했습니다."),
	ACCEPTED(202, "S_202", "요청이 접수되었습니다.");

	private final int status;
	private final String code;
	private final String message;

	SuccessCode(int status, String code, String message) {
		this.status = status;
		this.code = code;
		this.message = message;
	}

	public int status() {
		return status;
	}

	public String code() {
		return code;
	}

	public String message() {
		return message;
	}

	public static SuccessCode fromStatus(int status) {
		return switch (status) {
			case 201 -> CREATED;
			case 202 -> ACCEPTED;
			default -> OK;
		};
	}
}
