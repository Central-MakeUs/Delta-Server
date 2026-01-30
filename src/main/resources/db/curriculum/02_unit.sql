CREATE TABLE unit (
	id VARCHAR(50) NOT NULL,
	name VARCHAR(100) NOT NULL,
	parent_id VARCHAR(50) NULL,
	sort_order INT NOT NULL,
	is_active TINYINT(1) NOT NULL,
	PRIMARY KEY (id),
	KEY idx_unit_parent (parent_id),
	KEY idx_unit_active_sort (is_active, sort_order),
	CONSTRAINT fk_unit_parent_id FOREIGN KEY (parent_id) REFERENCES unit (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
