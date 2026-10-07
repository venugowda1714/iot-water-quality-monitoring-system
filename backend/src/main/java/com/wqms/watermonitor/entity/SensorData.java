package com.wqms.watermonitor.entity;

import com.wqms.watermonitor.enums.RelayStatus;
import com.wqms.watermonitor.enums.WaterQualityStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity representing a single sensor reading captured from the ESP32
 * device, along with the derived water quality status and relay state
 * at the time of reading.
 */
@Entity
@Table(name = "sensor_data")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SensorData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "device_id", nullable = false, length = 50)
    private String deviceId;

    @Column(name = "ph_value", nullable = false)
    private Double phValue;

    @Column(name = "tds_value", nullable = false)
    private Double tdsValue;

    @Column(name = "turbidity_value", nullable = false)
    private Double turbidityValue;

    @Column(name = "temperature_value", nullable = false)
    private Double temperatureValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private WaterQualityStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "relay_status", nullable = false, length = 5)
    private RelayStatus relayStatus;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    @PrePersist
    protected void onCreate() {
        if (recordedAt == null) {
            recordedAt = LocalDateTime.now();
        }
    }
}
