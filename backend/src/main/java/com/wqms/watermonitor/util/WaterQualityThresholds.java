package com.wqms.watermonitor.util;

/**
 * Central definition of the "safe" ranges for each sensor reading,
 * based on general drinking-water guidelines (WHO / BIS references).
 * Adjust these constants to match your local regulatory standards
 * or the specific use-case of the deployment.
 */
public final class WaterQualityThresholds {

    private WaterQualityThresholds() {
        // utility class - prevent instantiation
    }

    // pH: ideal drinking water range
    public static final double PH_MIN = 6.5;
    public static final double PH_MAX = 8.5;

    // TDS (Total Dissolved Solids) in ppm
    public static final double TDS_MIN = 50.0;
    public static final double TDS_MAX = 500.0;

    // Turbidity in NTU (Nephelometric Turbidity Units)
    public static final double TURBIDITY_MAX = 5.0;

    // Temperature in Celsius (comfortable / safe operating range)
    public static final double TEMPERATURE_MIN = 10.0;
    public static final double TEMPERATURE_MAX = 35.0;

    public static boolean isPhSafe(double ph) {
        return ph >= PH_MIN && ph <= PH_MAX;
    }

    public static boolean isTdsSafe(double tds) {
        return tds >= TDS_MIN && tds <= TDS_MAX;
    }

    public static boolean isTurbiditySafe(double turbidity) {
        return turbidity <= TURBIDITY_MAX && turbidity >= 0;
    }

    public static boolean isTemperatureSafe(double temperature) {
        return temperature >= TEMPERATURE_MIN && temperature <= TEMPERATURE_MAX;
    }
}
