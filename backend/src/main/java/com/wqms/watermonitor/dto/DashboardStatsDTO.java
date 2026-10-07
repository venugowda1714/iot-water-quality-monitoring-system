package com.wqms.watermonitor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Aggregate statistics shown on the dashboard summary cards.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardStatsDTO {
    private long totalReadings;
    private long safeReadingsCount;
    private long unsafeReadingsCount;
    private double safePercentage;
    private Double avgPh;
    private Double avgTds;
    private Double avgTurbidity;
    private Double avgTemperature;
}
