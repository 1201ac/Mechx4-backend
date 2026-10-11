CREATE TABLE shipment_assignments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    shipment_id BIGINT NOT NULL,
    driver_user_id BIGINT NOT NULL,
    vehicle_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    assigned_by_user_id BIGINT NOT NULL,
    decline_reason TEXT,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    responded_at TIMESTAMP,
    CONSTRAINT fk_assignment_shipment FOREIGN KEY (shipment_id) REFERENCES shipments(id) ON DELETE CASCADE,
    CONSTRAINT fk_assignment_driver FOREIGN KEY (driver_user_id) REFERENCES users(id),
    CONSTRAINT fk_assignment_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles(id),
    CONSTRAINT fk_assignment_moderator FOREIGN KEY (assigned_by_user_id) REFERENCES users(id)
);

CREATE INDEX idx_assignment_shipment ON shipment_assignments(shipment_id);
CREATE INDEX idx_assignment_driver ON shipment_assignments(driver_user_id);
CREATE INDEX idx_assignment_status ON shipment_assignments(status);
