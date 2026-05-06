package cmc.delta.domain.stats.model;

import cmc.delta.domain.user.model.User;
import cmc.delta.global.persistence.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
	name = "user_daily_access",
	uniqueConstraints = @UniqueConstraint(name = "uk_user_daily_access", columnNames = {"user_id", "access_date"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserDailyAccess extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_uda_user"))
	private User user;

	@Column(name = "access_date", nullable = false)
	private LocalDate accessDate;

	public static UserDailyAccess of(User user, LocalDate accessDate) {
		UserDailyAccess entity = new UserDailyAccess();
		entity.user = user;
		entity.accessDate = accessDate;
		return entity;
	}
}
