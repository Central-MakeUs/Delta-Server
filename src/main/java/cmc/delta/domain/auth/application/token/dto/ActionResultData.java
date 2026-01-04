package cmc.delta.domain.auth.application.token.dto;

public record ActionResultData(boolean reissued) {

	public static ActionResultData success() {
		return new ActionResultData(true);
	}
}
