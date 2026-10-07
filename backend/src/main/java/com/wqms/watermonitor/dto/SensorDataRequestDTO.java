package com.wqms.watermonitor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO used by the ESP32 device to submit a new sensor reading
 * via POST /api/v1/sensor-data
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SensorDataRequestDTO {

    @NotBlank(message = "deviceId is required")
    private String deviceId;

    @NotNull(message = "phValue is required")
    private Double phValue;

    @NotNull(message = "tdsValue is required")
    private Double tdsValue;

    @NotNull(message = "turbidityValue is required")
    private Double turbidityValue;

    @NotNull(message = "temperatureValue is required")
    private Double temperatureValue;
}
