package cmc.delta.domain.user.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

import cmc.delta.domain.user.model.enums.UserStatus;
import cmc.delta.global.persistence.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "users", indexes = {
	@Index(name = "idx_users_status", columnList = "status")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "email", length = 320)
	private String email;

	@Column(name = "nickname", length = 50)
	private String nickname;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	private UserStatus status;

	@Deprecated
	@Column(name = "name", length = 50)
	private String name;

	@Column(name = "birth_date")
	private LocalDate birthDate;

	@Column(name = "terms_agreed_at")
	private Instant termsAgreedAt;

	@Column(name = "profile_image_storage_key", length = 512)
	private String profileImageStorageKey;

	@Column(name = "withdrawn_at")
	private Instant withdrawnAt;

	private User(String email, String nickname, UserStatus status) {
		this.email = normalize(email);
		this.nickname = normalize(nickname);
		this.status = status;
	}

	public static User createProvisioned(String email, String nickname) {
		return new User(email, nickname, UserStatus.ONBOARDING_REQUIRED);
	}

	public boolean isWithdrawn() {
		return this.status == UserStatus.WITHDRAWN;
	}

	public void withdraw() {
		this.status = UserStatus.WITHDRAWN;
		if (this.withdrawnAt == null) {
			this.withdrawnAt = Instant.now();
		}
	}


    public void updateProfileImage(String storageKey) {
        this.profileImageStorageKey = normalize(storageKey);
    }

	public void clearProfileImage() {
		this.profileImageStorageKey = null;
	}

	public void syncProfile(String email, String nickname) {
		String nextEmail = normalize(email);
		if (nextEmail != null && !Objects.equals(this.email, nextEmail)) {
			this.email = nextEmail;
		}

		String nextNickname = normalize(nickname);
		if (nextNickname != null && !Objects.equals(this.nickname, nextNickname)) {
			this.nickname = nextNickname;
		}
	}

	public void completeOnboarding(String nicknameOrName, LocalDate birthDate, Instant agreedAt) {
		this.nickname = normalize(nicknameOrName);
		this.birthDate = birthDate;

		if (this.termsAgreedAt == null) {
			this.termsAgreedAt = agreedAt;
		}

		if (this.status == UserStatus.ONBOARDING_REQUIRED) {
			this.status = UserStatus.ACTIVE;
		}
	}

	@Deprecated
	public void updateName(String nickname) {
		updateNickname(nickname);
	}

	public void updateNickname(String nickname) {
		this.nickname = normalize(nickname);
	}

	private static String normalize(String value) {
		if (value == null)
			return null;
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
