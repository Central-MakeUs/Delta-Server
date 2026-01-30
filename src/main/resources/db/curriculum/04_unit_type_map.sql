CREATE TABLE unit_type_map (
	unit_id VARCHAR(50) NOT NULL,
	type_id VARCHAR(50) NOT NULL,
	PRIMARY KEY (unit_id, type_id),
	KEY idx_unit_type_map_type_unit (type_id, unit_id),
	CONSTRAINT fk_unit_type_map_unit_id FOREIGN KEY (unit_id) REFERENCES unit (id),
	CONSTRAINT fk_unit_type_map_type_id FOREIGN KEY (type_id) REFERENCES problem_type (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
