package com.wqms.watermonitor.service;

import com.wqms.watermonitor.dto.DashboardStatsDTO;
import com.wqms.watermonitor.dto.SensorDataRequestDTO;
import com.wqms.watermonitor.dto.SensorDataResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Business logic contract for handling sensor readings:
 * validation, persistence, water-quality evaluation, and relay control.
 */
public interface SensorDataService {

    /**
     * Validates, evaluates, and persists a new sensor reading.
     * Determines whether the reading is SAFE/UNSAFE and whether the
     * purification relay should be ON or OFF, then returns the full
     * result (including relay instruction) to the caller (ESP32).
     */
    SensorDataResponseDTO ingestSensorData(SensorDataRequestDTO requestDTO);

    /**
     * Returns the most recent reading for live dashboard display.
     */
    SensorDataResponseDTO getLatestReading();

    /**
     * Returns paginated historical readings, most recent first.
     */
    Page<SensorDataResponseDTO> getHistory(Pageable pageable);

    /**
     * Returns the last 50 readings (oldest-first) for chart trend rendering.
     */
    List<SensorDataResponseDTO> getTrendData();

    /**
     * Returns aggregate statistics (counts, averages, safe percentage)
     * computed over the last 24 hours.
     */
    DashboardStatsDTO getDashboardStats();

    /**
     * Returns the current relay state (ON/OFF) derived from the most
     * recent reading, useful for a lightweight ESP32 polling endpoint.
     */
    String getCurrentRelayStatus();
}
