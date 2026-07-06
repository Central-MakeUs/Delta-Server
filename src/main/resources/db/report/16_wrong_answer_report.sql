CREATE TABLE wrong_answer_report (
	id BIGINT NOT NULL AUTO_INCREMENT,
	user_id BIGINT NOT NULL,
	status VARCHAR(20) NOT NULL,
	input_hash VARCHAR(64) NOT NULL,
	problem_count BIGINT NOT NULL,
	requested_at DATETIME(6) NOT NULL,
	started_at DATETIME(6) NULL,
	completed_at DATETIME(6) NULL,
	aggregate_json MEDIUMTEXT NULL,
	narrative_json MEDIUMTEXT NULL,
	failure_reason VARCHAR(255) NULL,
	created_at DATETIME(6) NOT NULL,
	updated_at DATETIME(6) NOT NULL,
	PRIMARY KEY (id),
	KEY idx_wrong_answer_report_user_id (user_id, id),
	KEY idx_wrong_answer_report_status_requested (status, requested_at),
	CONSTRAINT fk_wrong_answer_report_user_id FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
