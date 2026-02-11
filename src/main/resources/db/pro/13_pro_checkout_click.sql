CREATE TABLE pro_checkout_click (
	id BIGINT NOT NULL AUTO_INCREMENT,
	user_id BIGINT NOT NULL,
	created_at DATETIME(6) NOT NULL,
	updated_at DATETIME(6) NOT NULL,
	PRIMARY KEY (id),
	KEY idx_pro_checkout_click_user_created (user_id, created_at),
	KEY idx_pro_checkout_click_created (created_at),
	CONSTRAINT fk_pro_checkout_click_user_id FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
