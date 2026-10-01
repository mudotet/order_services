ALTER TABLE notifications ADD COLUMN order_id CHAR(36);
ALTER TABLE notifications ADD COLUMN customer_name VARCHAR(255);
ALTER TABLE notifications ADD COLUMN customer_email VARCHAR(255);
ALTER TABLE notifications ADD COLUMN message VARCHAR(500);
ALTER TABLE notifications ADD COLUMN processed_at DATETIME;
ALTER TABLE notifications ADD COLUMN attempts INT NOT NULL DEFAULT 0;
ALTER TABLE notifications ADD COLUMN last_error VARCHAR(500);
CREATE INDEX idx_notifications_pending ON notifications (processed_at, deleted, created_at);
