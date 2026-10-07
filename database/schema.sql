-- =========================================================
-- Automatic Water Quality Monitoring System
-- Database schema
-- =========================================================
-- Run this script in MySQL before starting the Spring Boot
-- application (or let Hibernate auto-create it - see
-- application.properties, ddl-auto=update - this script is
-- provided for explicit/manual setup and documentation).
-- =========================================================

CREATE DATABASE IF NOT EXISTS water_quality_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE water_quality_db;

DROP TABLE IF EXISTS sensor_data;

CREATE TABLE sensor_data (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_id          VARCHAR(50)     NOT NULL,
    ph_value           DOUBLE          NOT NULL,
    tds_value          DOUBLE          NOT NULL,
    turbidity_value    DOUBLE          NOT NULL,
    temperature_value  DOUBLE          NOT NULL,
    status             VARCHAR(10)     NOT NULL COMMENT 'SAFE or UNSAFE',
    relay_status       VARCHAR(5)      NOT NULL COMMENT 'ON or OFF',
    recorded_at        DATETIME        NOT NULL,

    INDEX idx_recorded_at (recorded_at),
    INDEX idx_device_id (device_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
