CREATE TABLE problem_choice (
	id BIGINT NOT NULL AUTO_INCREMENT,
	problem_id BIGINT NOT NULL,
	choice_no INT NOT NULL,
	label VARCHAR(10) NOT NULL,
	text MEDIUMTEXT NOT NULL,
	PRIMARY KEY (id),
	UNIQUE KEY uk_problem_choice (problem_id, choice_no),
	CONSTRAINT fk_problem_choice_problem_id FOREIGN KEY (problem_id) REFERENCES problem (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
