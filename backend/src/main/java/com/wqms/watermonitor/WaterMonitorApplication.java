package com.wqms.watermonitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the Automatic Water Quality Monitoring System.
 *
 * This Spring Boot application exposes REST APIs that receive real-time
 * sensor readings (pH, TDS, Turbidity, Temperature) from an ESP32 device,
 * validates and persists them to MySQL, evaluates water quality against
 * safety thresholds, and instructs the ESP32 (via the relay-status field
 * in the API response) whether to activate the purification relay.
 *
 * @author WQMS Team
 */
@SpringBootApplication
@EnableScheduling
public class WaterMonitorApplication {

    public static void main(String[] args) {
        SpringApplication.run(WaterMonitorApplication.class, args);
    }
}
