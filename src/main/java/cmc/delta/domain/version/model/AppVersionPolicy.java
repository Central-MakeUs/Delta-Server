package cmc.delta.domain.version.model;

import cmc.delta.global.persistence.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "app_version_policy")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AppVersionPolicy extends BaseTimeEntity {

	public static final Long DEFAULT_POLICY_ID = 1L;
	public static final String DEFAULT_VERSION = "1.0.0";

	@Id
	private Long id;

	@Column(name = "minimum_version", nullable = false, length = 32)
	private String minimumVersion;

	@Column(name = "latest_version", nullable = false, length = 32)
	private String latestVersion;

	private AppVersionPolicy(Long id, String minimumVersion, String latestVersion) {
		this.id = id;
		this.minimumVersion = minimumVersion;
		this.latestVersion = latestVersion;
	}

	public static AppVersionPolicy create(Long id, String minimumVersion, String latestVersion) {
		return new AppVersionPolicy(id, minimumVersion, latestVersion);
	}

	public static AppVersionPolicy createDefault() {
		return create(DEFAULT_POLICY_ID, DEFAULT_VERSION, DEFAULT_VERSION);
	}
}
