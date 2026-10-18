CREATE TABLE daily_memos (
    id VARCHAR(255) PRIMARY KEY,
    created_by VARCHAR(40) NOT NULL,
    content VARCHAR(500) NOT NULL,
    created_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_daily_memos_created_at ON daily_memos(created_at);

CREATE TABLE event_reminder_deliveries (
    id VARCHAR(255) PRIMARY KEY,
    delivered_at TIMESTAMP
);
