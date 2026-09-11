CREATE TABLE IF NOT EXISTS app_visits (
    id VARCHAR(36) PRIMARY KEY,
    username VARCHAR(40) NOT NULL,
    nickname VARCHAR(40),
    visited_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_app_visits_username
    ON app_visits (username);

CREATE INDEX IF NOT EXISTS idx_app_visits_visited_at
    ON app_visits (visited_at);
