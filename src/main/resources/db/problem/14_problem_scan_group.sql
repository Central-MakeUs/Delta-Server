CREATE TABLE problem_scan_group (
	id         BIGINT      NOT NULL AUTO_INCREMENT,
	user_id    BIGINT      NOT NULL,
	created_at DATETIME(6) NOT NULL,
	PRIMARY KEY (id),
	KEY idx_problem_scan_group_user_created (user_id, created_at),
	CONSTRAINT fk_problem_scan_group_user_id FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

ALTER TABLE problem_scan
	ADD COLUMN scan_group_id BIGINT NULL AFTER user_id,
	ADD KEY idx_problem_scan_group_id (scan_group_id),
	ADD CONSTRAINT fk_problem_scan_group_id
		FOREIGN KEY (scan_group_id) REFERENCES problem_scan_group (id);
