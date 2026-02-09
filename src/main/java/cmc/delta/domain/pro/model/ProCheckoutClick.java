package cmc.delta.domain.pro.model;

import cmc.delta.global.persistence.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "pro_checkout_click", indexes = {
	@Index(name = "idx_pro_checkout_click_user_created", columnList = "user_id, created_at"),
	@Index(name = "idx_pro_checkout_click_created", columnList = "created_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProCheckoutClick extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	private ProCheckoutClick(Long userId) {
		this.userId = userId;
	}

	public static ProCheckoutClick create(Long userId) {
		return new ProCheckoutClick(userId);
	}
}
