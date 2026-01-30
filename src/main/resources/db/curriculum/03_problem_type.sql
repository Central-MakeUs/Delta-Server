CREATE TABLE problem_type (
	id VARCHAR(50) NOT NULL,
	name VARCHAR(100) NOT NULL,
	sort_order INT NOT NULL,
	is_active TINYINT(1) NOT NULL,
	created_by_user_id BIGINT NULL,
	is_custom TINYINT(1) NOT NULL,
	PRIMARY KEY (id),
	UNIQUE KEY uk_type_user_name (created_by_user_id, name),
	KEY idx_type_active_sort (is_active, sort_order),
	KEY idx_type_custom_sort (created_by_user_id, is_custom, sort_order),
	CONSTRAINT fk_problem_type_created_by_user_id FOREIGN KEY (created_by_user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
