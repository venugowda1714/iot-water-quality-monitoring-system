package com.wqms.watermonitor.dto;

import com.wqms.watermonitor.enums.RelayStatus;
import com.wqms.watermonitor.enums.WaterQualityStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO returned to clients (dashboard and ESP32) representing a
 * persisted sensor reading, including derived status/relay fields.
 *
 * The ESP32 reads the "relayStatus" field from this response after
 * every POST to know whether it should energize the relay pin.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SensorDataResponseDTO {

    private Long id;
    private String deviceId;
    private Double phValue;
    private Double tdsValue;
    private Double turbidityValue;
    private Double temperatureValue;
    private WaterQualityStatus status;
    private RelayStatus relayStatus;
    private LocalDateTime recordedAt;

    // Human-readable reasons explaining why the status is UNSAFE (if applicable)
    private String remarks;
}
