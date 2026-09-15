package cmc.delta.domain.version.application.port.in;

import cmc.delta.domain.version.application.port.in.result.AppVersionResponse;

public interface AppVersionQueryUseCase {

	AppVersionResponse getAppVersion(String currentVersion);
}
