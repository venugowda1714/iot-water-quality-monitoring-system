package com.wqms.watermonitor.service.impl;

import com.wqms.watermonitor.dto.DashboardStatsDTO;
import com.wqms.watermonitor.dto.SensorDataRequestDTO;
import com.wqms.watermonitor.dto.SensorDataResponseDTO;
import com.wqms.watermonitor.entity.SensorData;
import com.wqms.watermonitor.enums.RelayStatus;
import com.wqms.watermonitor.enums.WaterQualityStatus;
import com.wqms.watermonitor.exception.InvalidSensorDataException;
import com.wqms.watermonitor.exception.ResourceNotFoundException;
import com.wqms.watermonitor.repository.SensorDataRepository;
import com.wqms.watermonitor.service.SensorDataService;
import com.wqms.watermonitor.util.WaterQualityThresholds;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Core business logic implementation.
 *
 * Responsible for:
 *  1. Sanity-validating incoming sensor values.
 *  2. Comparing each reading against safe thresholds.
 *  3. Deriving an overall SAFE/UNSAFE status.
 *  4. Deciding relay ON/OFF (ON = activate purification when UNSAFE,
 *     OFF = deactivate once readings return to safe range).
 *  5. Persisting the reading and returning it to the caller.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SensorDataServiceImpl implements SensorDataService {

    private final SensorDataRepository sensorDataRepository;

    @Override
    @Transactional
    public SensorDataResponseDTO ingestSensorData(SensorDataRequestDTO requestDTO) {
        validateRawRanges(requestDTO);

        StringBuilder remarks = new StringBuilder();
        boolean phSafe = WaterQualityThresholds.isPhSafe(requestDTO.getPhValue());
        boolean tdsSafe = WaterQualityThresholds.isTdsSafe(requestDTO.getTdsValue());
        boolean turbiditySafe = WaterQualityThresholds.isTurbiditySafe(requestDTO.getTurbidityValue());
        boolean temperatureSafe = WaterQualityThresholds.isTemperatureSafe(requestDTO.getTemperatureValue());

        if (!phSafe) {
            remarks.append(String.format("pH (%.2f) out of safe range [%.1f-%.1f]. ",
                    requestDTO.getPhValue(), WaterQualityThresholds.PH_MIN, WaterQualityThresholds.PH_MAX));
        }
        if (!tdsSafe) {
            remarks.append(String.format("TDS (%.2f ppm) out of safe range [%.1f-%.1f]. ",
                    requestDTO.getTdsValue(), WaterQualityThresholds.TDS_MIN, WaterQualityThresholds.TDS_MAX));
        }
        if (!turbiditySafe) {
            remarks.append(String.format("Turbidity (%.2f NTU) exceeds safe max %.1f. ",
                    requestDTO.getTurbidityValue(), WaterQualityThresholds.TURBIDITY_MAX));
        }
        if (!temperatureSafe) {
            remarks.append(String.format("Temperature (%.2f C) out of safe range [%.1f-%.1f]. ",
                    requestDTO.getTemperatureValue(), WaterQualityThresholds.TEMPERATURE_MIN, WaterQualityThresholds.TEMPERATURE_MAX));
        }

        boolean overallSafe = phSafe && tdsSafe && turbiditySafe && temperatureSafe;
        WaterQualityStatus status = overallSafe ? WaterQualityStatus.SAFE : WaterQualityStatus.UNSAFE;

        // Business rule: relay turns ON (purification active) when unsafe,
        // and OFF automatically once values return to the safe range.
        RelayStatus relayStatus = overallSafe ? RelayStatus.OFF : RelayStatus.ON;

        if (!overallSafe) {
            log.warn("UNSAFE water quality detected for device [{}]: {}", requestDTO.getDeviceId(), remarks);
        }

        SensorData entity = SensorData.builder()
                .deviceId(requestDTO.getDeviceId())
                .phValue(requestDTO.getPhValue())
                .tdsValue(requestDTO.getTdsValue())
                .turbidityValue(requestDTO.getTurbidityValue())
                .temperatureValue(requestDTO.getTemperatureValue())
                .status(status)
                .relayStatus(relayStatus)
                .recordedAt(LocalDateTime.now())
                .build();

        SensorData saved = sensorDataRepository.save(entity);

        return toResponseDTO(saved, overallSafe ? "All parameters within safe range." : remarks.toString().trim());
    }

    @Override
    public SensorDataResponseDTO getLatestReading() {
        SensorData latest = sensorDataRepository.findTopByOrderByRecordedAtDesc()
                .orElseThrow(() -> new ResourceNotFoundException("No sensor readings found yet."));
        return toResponseDTO(latest, null);
    }

    @Override
    public Page<SensorDataResponseDTO> getHistory(Pageable pageable) {
        Page<SensorData> page = sensorDataRepository.findAllByOrderByRecordedAtDesc(pageable);
        return page.map(sd -> toResponseDTO(sd, null));
    }

    @Override
    public List<SensorDataResponseDTO> getTrendData() {
        List<SensorData> data = sensorDataRepository.findTop50ByOrderByRecordedAtDesc();
        List<SensorDataResponseDTO> result = data.stream()
                .map(sd -> toResponseDTO(sd, null))
                .collect(Collectors.toList());
        // reverse so charts render oldest -> newest, left to right
        List<SensorDataResponseDTO> chronological = new ArrayList<>(result);
        java.util.Collections.reverse(chronological);
        return chronological;
    }

    @Override
    public DashboardStatsDTO getDashboardStats() {
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        long safeCount = sensorDataRepository.countByStatus(WaterQualityStatus.SAFE);
        long unsafeCount = sensorDataRepository.countByStatus(WaterQualityStatus.UNSAFE);
        long total = safeCount + unsafeCount;
        double safePct = total == 0 ? 0.0 : (safeCount * 100.0) / total;

        return DashboardStatsDTO.builder()
                .totalReadings(total)
                .safeReadingsCount(safeCount)
                .unsafeReadingsCount(unsafeCount)
                .safePercentage(Math.round(safePct * 100.0) / 100.0)
                .avgPh(round(sensorDataRepository.findAveragePhSince(since)))
                .avgTds(round(sensorDataRepository.findAverageTdsSince(since)))
                .avgTurbidity(round(sensorDataRepository.findAverageTurbiditySince(since)))
                .avgTemperature(round(sensorDataRepository.findAverageTemperatureSince(since)))
                .build();
    }

    @Override
    public String getCurrentRelayStatus() {
        return sensorDataRepository.findTopByOrderByRecordedAtDesc()
                .map(sd -> sd.getRelayStatus().name())
                .orElse(RelayStatus.OFF.name());
    }

    // ---------- helpers ----------

    private void validateRawRanges(SensorDataRequestDTO dto) {
        if (dto.getPhValue() < 0 || dto.getPhValue() > 14) {
            throw new InvalidSensorDataException("pH value must be between 0 and 14.");
        }
        if (dto.getTdsValue() < 0 || dto.getTdsValue() > 5000) {
            throw new InvalidSensorDataException("TDS value must be between 0 and 5000 ppm.");
        }
        if (dto.getTurbidityValue() < 0 || dto.getTurbidityValue() > 3000) {
            throw new InvalidSensorDataException("Turbidity value must be between 0 and 3000 NTU.");
        }
        if (dto.getTemperatureValue() < -10 || dto.getTemperatureValue() > 100) {
            throw new InvalidSensorDataException("Temperature value must be between -10 and 100 C.");
        }
    }

    private Double round(Double value) {
        if (value == null) return null;
        return Math.round(value * 100.0) / 100.0;
    }

    private SensorDataResponseDTO toResponseDTO(SensorData entity, String remarksOverride) {
        return SensorDataResponseDTO.builder()
                .id(entity.getId())
                .deviceId(entity.getDeviceId())
                .phValue(entity.getPhValue())
                .tdsValue(entity.getTdsValue())
                .turbidityValue(entity.getTurbidityValue())
                .temperatureValue(entity.getTemperatureValue())
                .status(entity.getStatus())
                .relayStatus(entity.getRelayStatus())
                .recordedAt(entity.getRecordedAt())
                .remarks(remarksOverride)
                .build();
    }
}
