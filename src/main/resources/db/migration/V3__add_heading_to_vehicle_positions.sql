-- V3 Add missing heading column to vehicle_positions table

ALTER TABLE vehicle_positions ADD COLUMN heading DOUBLE PRECISION DEFAULT 0.0;
