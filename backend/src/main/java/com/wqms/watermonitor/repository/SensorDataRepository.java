package com.wqms.watermonitor.repository;

import com.wqms.watermonitor.entity.SensorData;
import com.wqms.watermonitor.enums.WaterQualityStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link SensorData}.
 * Provides CRUD operations plus several derived/custom queries used
 * by the dashboard (latest reading, history, trends, statistics).
 */
@Repository
public interface SensorDataRepository extends JpaRepository<SensorData, Long> {

    /**
     * Fetches the single most-recent reading for a given device,
     * used to display "live" values on the dashboard.
     */
    Optional<SensorData> findTopByDeviceIdOrderByRecordedAtDesc(String deviceId);

    /**
     * Fetches the single most-recent reading overall.
     */
    Optional<SensorData> findTopByOrderByRecordedAtDesc();

    /**
     * Paginated historical data, most recent first.
     */
    Page<SensorData> findAllByOrderByRecordedAtDesc(Pageable pageable);

    /**
     * Readings within a time window, used for trend charts.
     */
    List<SensorData> findByRecordedAtBetweenOrderByRecordedAtAsc(LocalDateTime start, LocalDateTime end);

    long countByStatus(WaterQualityStatus status);

    @Query("SELECT AVG(s.phValue) FROM SensorData s WHERE s.recordedAt >= :since")
    Double findAveragePhSince(@Param("since") LocalDateTime since);

    @Query("SELECT AVG(s.tdsValue) FROM SensorData s WHERE s.recordedAt >= :since")
    Double findAverageTdsSince(@Param("since") LocalDateTime since);

    @Query("SELECT AVG(s.turbidityValue) FROM SensorData s WHERE s.recordedAt >= :since")
    Double findAverageTurbiditySince(@Param("since") LocalDateTime since);

    @Query("SELECT AVG(s.temperatureValue) FROM SensorData s WHERE s.recordedAt >= :since")
    Double findAverageTemperatureSince(@Param("since") LocalDateTime since);

    /**
     * Latest N readings, oldest-first, convenient for chart plotting.
     */
    List<SensorData> findTop50ByOrderByRecordedAtDesc();
}
