package com.wqms.watermonitor;

import com.wqms.watermonitor.dto.SensorDataRequestDTO;
import com.wqms.watermonitor.dto.SensorDataResponseDTO;
import com.wqms.watermonitor.entity.SensorData;
import com.wqms.watermonitor.enums.RelayStatus;
import com.wqms.watermonitor.enums.WaterQualityStatus;
import com.wqms.watermonitor.repository.SensorDataRepository;
import com.wqms.watermonitor.service.impl.SensorDataServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Basic unit tests validating the core SAFE/UNSAFE and relay-control
 * business logic in isolation, using a mocked repository.
 */
class SensorDataServiceTest {

    private SensorDataRepository repository;
    private SensorDataServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(SensorDataRepository.class);
        service = new SensorDataServiceImpl(repository);
    }

    @Test
    void safeReadingsShouldTurnRelayOff() {
        SensorDataRequestDTO request = new SensorDataRequestDTO("ESP32-01", 7.0, 200.0, 2.0, 25.0);

        when(repository.save(any(SensorData.class))).thenAnswer(invocation -> {
            SensorData sd = invocation.getArgument(0);
            sd.setId(1L);
            return sd;
        });

        SensorDataResponseDTO response = service.ingestSensorData(request);

        assertEquals(WaterQualityStatus.SAFE, response.getStatus());
        assertEquals(RelayStatus.OFF, response.getRelayStatus());
    }

    @Test
    void unsafeReadingShouldTurnRelayOn() {
        // pH too low, turbidity too high
        SensorDataRequestDTO request = new SensorDataRequestDTO("ESP32-01", 4.0, 200.0, 15.0, 25.0);

        when(repository.save(any(SensorData.class))).thenAnswer(invocation -> {
            SensorData sd = invocation.getArgument(0);
            sd.setId(2L);
            return sd;
        });

        SensorDataResponseDTO response = service.ingestSensorData(request);

        assertEquals(WaterQualityStatus.UNSAFE, response.getStatus());
        assertEquals(RelayStatus.ON, response.getRelayStatus());
        assertNotNull(response.getRemarks());
        assertFalse(response.getRemarks().isEmpty());
    }
}
