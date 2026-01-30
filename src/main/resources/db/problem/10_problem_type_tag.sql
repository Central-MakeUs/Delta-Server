CREATE TABLE problem_type_tag (
	problem_id BIGINT NOT NULL,
	type_id VARCHAR(50) NOT NULL,
	PRIMARY KEY (problem_id, type_id),
	UNIQUE KEY uk_problem_type_tag (problem_id, type_id),
	KEY idx_problem_type_tag_type_problem (type_id, problem_id),
	KEY idx_problem_type_tag_problem (problem_id),
	CONSTRAINT fk_problem_type_tag_problem_id FOREIGN KEY (problem_id) REFERENCES problem (id),
	CONSTRAINT fk_problem_type_tag_type_id FOREIGN KEY (type_id) REFERENCES problem_type (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
