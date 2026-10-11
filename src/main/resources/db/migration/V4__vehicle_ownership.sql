ALTER TABLE vehicles ADD COLUMN owner_user_id BIGINT;
ALTER TABLE vehicles ADD CONSTRAINT fk_vehicles_owner_user FOREIGN KEY (owner_user_id) REFERENCES users(id);
CREATE INDEX idx_vehicles_owner_user ON vehicles(owner_user_id);
