package cmc.delta.domain.version.application.port.in.result;

public record AppVersionResponse(
	String minimumVersion,
	String latestVersion,
	boolean forceUpdate,
	boolean updateAvailable) {
}
