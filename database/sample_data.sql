-- =========================================================
-- Sample data for the sensor_data table.
-- Useful for testing the dashboard without waiting for the
-- ESP32 hardware to send real readings.
-- Run AFTER schema.sql.
-- =========================================================

USE water_quality_db;

INSERT INTO sensor_data
    (device_id, ph_value, tds_value, turbidity_value, temperature_value, status, relay_status, recorded_at)
VALUES
    ('ESP32-01', 7.10, 210.5, 1.8, 24.5, 'SAFE',   'OFF', NOW() - INTERVAL 6 HOUR),
    ('ESP32-01', 7.05, 230.0, 2.1, 25.0, 'SAFE',   'OFF', NOW() - INTERVAL 5 HOUR),
    ('ESP32-01', 6.90, 480.0, 3.5, 26.2, 'SAFE',   'OFF', NOW() - INTERVAL 4 HOUR),
    ('ESP32-01', 5.80, 520.0, 7.2, 27.0, 'UNSAFE', 'ON',  NOW() - INTERVAL 3 HOUR),
    ('ESP32-01', 5.95, 510.0, 6.5, 27.4, 'UNSAFE', 'ON',  NOW() - INTERVAL 2 HOUR),
    ('ESP32-01', 6.80, 460.0, 4.0, 26.0, 'SAFE',   'OFF', NOW() - INTERVAL 1 HOUR),
    ('ESP32-01', 7.20, 300.0, 2.2, 24.8, 'SAFE',   'OFF', NOW() - INTERVAL 30 MINUTE),
    ('ESP32-01', 7.15, 280.0, 1.9, 24.6, 'SAFE',   'OFF', NOW() - INTERVAL 10 MINUTE),
    ('ESP32-01', 7.08, 260.0, 2.0, 24.7, 'SAFE',   'OFF', NOW());
