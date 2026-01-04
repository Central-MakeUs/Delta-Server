package cmc.delta.domain.user.model;

import cmc.delta.domain.user.model.enums.UserStatus;
import cmc.delta.global.persistence.BaseTimeEntity;
import jakarta.persistence.*;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
	name = "users",
	indexes = {
		@Index(name = "idx_users_status", columnList = "status")
	}
)
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

	private User(String email, String nickname) {
		this.email = normalize(email);
		this.nickname = normalize(nickname);
		this.status = UserStatus.ACTIVE;
	}

	public static User create(String email, String nickname) {
		return new User(email, nickname);
	}

	public boolean isWithdrawn() {
		return this.status == UserStatus.WITHDRAWN;
	}

	public void withdraw() {
		this.status = UserStatus.WITHDRAWN;
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

	private static String normalize(String value) {
		if (value == null) return null;
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
