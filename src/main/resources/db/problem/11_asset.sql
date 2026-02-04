CREATE TABLE asset (
	id BIGINT NOT NULL AUTO_INCREMENT,
	scan_id BIGINT NOT NULL,
	asset_type VARCHAR(30) NOT NULL,
	slot INT NOT NULL,
	storage_key VARCHAR(255) NOT NULL,
	width INT NULL,
	height INT NULL,
	meta JSON NULL,
	created_at DATETIME(6) NOT NULL,
	PRIMARY KEY (id),
	UNIQUE KEY uk_asset_scan_type_slot (scan_id, asset_type, slot),
	UNIQUE KEY idx_asset_storage_key (storage_key),
	KEY idx_asset_scan_type (scan_id, asset_type),
	CONSTRAINT fk_asset_scan_id FOREIGN KEY (scan_id) REFERENCES problem_scan (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
