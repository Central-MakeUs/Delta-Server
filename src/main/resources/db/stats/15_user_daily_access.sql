CREATE TABLE user_daily_access (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    user_id     BIGINT      NOT NULL,
    access_date DATE        NOT NULL,
    created_at  DATETIME(6) NOT NULL,
    updated_at  DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_daily_access (user_id, access_date),
    CONSTRAINT fk_uda_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
