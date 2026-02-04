CREATE TABLE social_accounts (
	id BIGINT NOT NULL AUTO_INCREMENT,
	provider VARCHAR(20) NOT NULL,
	provider_user_id VARCHAR(100) NOT NULL,
	user_id BIGINT NOT NULL,
	created_at DATETIME(6) NOT NULL,
	updated_at DATETIME(6) NOT NULL,
	PRIMARY KEY (id),
	UNIQUE KEY uk_social_accounts_provider_user (provider, provider_user_id),
	UNIQUE KEY uk_social_accounts_provider_user_id (provider, user_id),
	KEY idx_social_accounts_user_id (user_id),
	CONSTRAINT fk_social_accounts_user_id FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
