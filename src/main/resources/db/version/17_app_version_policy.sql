CREATE TABLE app_version_policy (
	id BIGINT NOT NULL,
	minimum_version VARCHAR(32) NOT NULL,
	latest_version VARCHAR(32) NOT NULL,
	created_at DATETIME(6) NOT NULL,
	updated_at DATETIME(6) NOT NULL,
	PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
