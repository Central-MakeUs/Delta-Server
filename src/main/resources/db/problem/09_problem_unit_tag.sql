CREATE TABLE problem_unit_tag (
	problem_id BIGINT NOT NULL,
	unit_id VARCHAR(50) NOT NULL,
	is_primary TINYINT(1) NOT NULL,
	PRIMARY KEY (problem_id, unit_id),
	KEY idx_problem_unit_tag_unit_problem (unit_id, problem_id),
	KEY idx_problem_unit_tag_primary (problem_id, is_primary),
	CONSTRAINT fk_problem_unit_tag_problem_id FOREIGN KEY (problem_id) REFERENCES problem (id),
	CONSTRAINT fk_problem_unit_tag_unit_id FOREIGN KEY (unit_id) REFERENCES unit (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
