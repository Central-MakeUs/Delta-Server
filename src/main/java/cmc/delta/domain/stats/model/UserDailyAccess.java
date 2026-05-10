package cmc.delta.domain.stats.model;

import cmc.delta.global.persistence.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "access_date", nullable = false)
	private LocalDate accessDate;
}
