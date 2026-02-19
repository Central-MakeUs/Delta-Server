CREATE TABLE users (
	id BIGINT NOT NULL AUTO_INCREMENT,
	email VARCHAR(320) NULL,
	nickname VARCHAR(50) NULL,
	status VARCHAR(20) NOT NULL,
	name VARCHAR(50) NULL,
	terms_agreed_at TIMESTAMP(6) NULL,
	profile_image_storage_key VARCHAR(512) NULL,
	withdrawn_at TIMESTAMP(6) NULL,
	created_at DATETIME(6) NOT NULL,
	updated_at DATETIME(6) NOT NULL,
	PRIMARY KEY (id),
	KEY idx_users_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
