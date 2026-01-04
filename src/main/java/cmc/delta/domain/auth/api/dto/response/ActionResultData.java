package cmc.delta.domain.auth.api.dto.response;

public record ActionResultData(boolean reissued) {

	public static ActionResultData success() {
		return new ActionResultData(true);
	}
}
