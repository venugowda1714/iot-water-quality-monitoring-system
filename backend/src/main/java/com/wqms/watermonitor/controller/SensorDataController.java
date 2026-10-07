package com.wqms.watermonitor.controller;

import com.wqms.watermonitor.dto.ApiResponse;
import com.wqms.watermonitor.dto.DashboardStatsDTO;
import com.wqms.watermonitor.dto.SensorDataRequestDTO;
import com.wqms.watermonitor.dto.SensorDataResponseDTO;
import com.wqms.watermonitor.service.SensorDataService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller exposing all sensor-data related endpoints, consumed
 * by both the ESP32 firmware (POST readings) and the web dashboard
 * (GET latest/history/trend/stats).
 *
 * Base path: /api/v1/sensor-data
 */
@RestController
@RequestMapping("/api/v1/sensor-data")
@RequiredArgsConstructor
public class SensorDataController {

    private final SensorDataService sensorDataService;

    /**
     * Receives a new sensor reading from the ESP32.
     * The response's "relayStatus" field tells the ESP32 whether to
     * energize the purification relay.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<SensorDataResponseDTO>> submitReading(
            @Valid @RequestBody SensorDataRequestDTO requestDTO) {
        SensorDataResponseDTO result = sensorDataService.ingestSensorData(requestDTO);
        String message = result.getStatus().name().equals("SAFE")
                ? "Reading recorded. Water quality is SAFE."
                : "Reading recorded. Water quality is UNSAFE - relay activated.";
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(message, result));
    }

    /**
     * Returns the most recent reading, used to populate the "live" cards
     * on the dashboard.
     */
    @GetMapping("/latest")
    public ResponseEntity<ApiResponse<SensorDataResponseDTO>> getLatest() {
        SensorDataResponseDTO latest = sensorDataService.getLatestReading();
        return ResponseEntity.ok(ApiResponse.success("Latest reading fetched.", latest));
    }

    /**
     * Returns paginated historical readings for the history table.
     * Example: GET /api/v1/sensor-data/history?page=0&size=20
     */
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<Page<SensorDataResponseDTO>>> getHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<SensorDataResponseDTO> history = sensorDataService.getHistory(pageable);
        return ResponseEntity.ok(ApiResponse.success("History fetched.", history));
    }

    /**
     * Returns the last 50 readings (chronological order) for chart trend rendering.
     */
    @GetMapping("/trend")
    public ResponseEntity<ApiResponse<List<SensorDataResponseDTO>>> getTrend() {
        List<SensorDataResponseDTO> trend = sensorDataService.getTrendData();
        return ResponseEntity.ok(ApiResponse.success("Trend data fetched.", trend));
    }

    /**
     * Returns aggregate dashboard statistics (safe %, averages, etc.).
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<DashboardStatsDTO>> getStats() {
        DashboardStatsDTO stats = sensorDataService.getDashboardStats();
        return ResponseEntity.ok(ApiResponse.success("Stats fetched.", stats));
    }

    /**
     * Lightweight endpoint the ESP32 can poll independently to check
     * the current relay state without submitting new data.
     */
    @GetMapping("/relay-status")
    public ResponseEntity<ApiResponse<Map<String, String>>> getRelayStatus() {
        String relayStatus = sensorDataService.getCurrentRelayStatus();
        return ResponseEntity.ok(ApiResponse.success("Relay status fetched.", Map.of("relayStatus", relayStatus)));
    }
}
