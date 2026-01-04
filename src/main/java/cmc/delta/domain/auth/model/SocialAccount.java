package cmc.delta.domain.auth.model;

import cmc.delta.domain.user.model.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
	name = "social_accounts",
	uniqueConstraints = {
		@UniqueConstraint(
			name = "uk_social_accounts_provider_user",
			columnNames = {"provider", "provider_user_id"}
		),
		@UniqueConstraint(
			name = "uk_social_accounts_provider_user_id",
			columnNames = {"provider", "user_id"}
		)
	},
	indexes = {
		@Index(name = "idx_social_accounts_user_id", columnList = "user_id")
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SocialAccount extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(name = "provider", nullable = false, length = 20)
	private SocialProvider provider;

	@Column(name = "provider_user_id", nullable = false, length = 100)
	private String providerUserId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	private SocialAccount(SocialProvider provider, String providerUserId, User user) {
		this.provider = provider;
		this.providerUserId = providerUserId;
		this.user = user;
	}

	public static SocialAccount link(SocialProvider provider, String providerUserId, User user) {
		return new SocialAccount(provider, providerUserId, user);
	}
}
