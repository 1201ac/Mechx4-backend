-- V2 Schema Updates and Performance Indexes

ALTER TABLE vehicles ADD COLUMN last_driver_fix_at TIMESTAMP;

CREATE INDEX idx_road_segments_status ON road_segments(status);
CREATE INDEX idx_road_segments_risk ON road_segments(risk_score);
CREATE INDEX idx_alerts_open_key ON alerts(open_key);
CREATE INDEX idx_alerts_status ON alerts(status);
CREATE INDEX idx_notifications_user ON notifications(user_id, read_at);
CREATE INDEX idx_vehicle_positions_veh_time ON vehicle_positions(vehicle_id, recorded_at);
CREATE INDEX idx_incidents_status_type ON incidents(status, type);
CREATE INDEX idx_field_reports_user_status ON field_reports(user_id, status);
CREATE INDEX idx_shipments_status ON shipments(status);
CREATE INDEX idx_shipments_vehicle ON shipments(vehicle_id);
CREATE INDEX idx_shipments_driver ON shipments(driver_user_id);

ALTER TABLE refresh_tokens ADD COLUMN family_id VARCHAR(64);
ALTER TABLE refresh_tokens ADD COLUMN revoked BOOLEAN DEFAULT FALSE;
CREATE INDEX idx_refresh_tokens_family ON refresh_tokens(family_id);
