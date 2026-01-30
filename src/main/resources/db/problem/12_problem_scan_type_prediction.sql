CREATE TABLE problem_scan_type_prediction (
	scan_id BIGINT NOT NULL,
	type_id VARCHAR(50) NOT NULL,
	rank_no INT NOT NULL,
	confidence DECIMAL(5, 4) NULL,
	PRIMARY KEY (scan_id, type_id),
	UNIQUE KEY uk_scan_rank (scan_id, rank_no),
	KEY idx_scan_rank (scan_id, rank_no),
	CONSTRAINT fk_problem_scan_type_prediction_scan_id FOREIGN KEY (scan_id) REFERENCES problem_scan (id),
	CONSTRAINT fk_problem_scan_type_prediction_type_id FOREIGN KEY (type_id) REFERENCES problem_type (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
