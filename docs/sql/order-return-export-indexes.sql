-- Run once on the existing database before large exports (ddl-auto is validate).
CREATE INDEX idx_order_returns_export ON order_returns (deleted, id DESC);
CREATE INDEX idx_order_returns_status_export ON order_returns (deleted, status, id DESC);
