package com.wqms.watermonitor.controller;

import com.wqms.watermonitor.dto.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Simple health-check endpoint useful for verifying the backend is up,
 * both during local development and for the ESP32 to confirm connectivity.
 */
@RestController
public class HealthController {

    @GetMapping("/api/v1/health")
    public ApiResponse<Map<String, String>> health() {
        return ApiResponse.success("Service is up.", Map.of("status", "UP"));
    }
}
