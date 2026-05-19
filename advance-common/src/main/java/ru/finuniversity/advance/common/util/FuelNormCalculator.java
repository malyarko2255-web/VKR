package ru.finuniversity.advance.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class FuelNormCalculator {

    private FuelNormCalculator() {}

    /**
     * Рассчитывает расход топлива по маршруту с поправочными коэффициентами.
     *
     * Коэффициенты (аддитивные):
     *   WINTER  → +10% (температура ниже -20°C)
     *   MOUNTAIN → +5% (горный рельеф)
     *   distanceKm > 100 → +5% (длинный маршрут)
     *
     * @return расход топлива в литрах
     */
    public static BigDecimal calculate(double distanceKm, double baseNorm,
                                       String season, String region) {
        double coefficient = 1.0;

        if ("WINTER".equalsIgnoreCase(season))    coefficient += 0.10;
        if ("MOUNTAIN".equalsIgnoreCase(region))  coefficient += 0.05;
        if (distanceKm > 100)                     coefficient += 0.05;

        double liters = distanceKm * baseNorm / 100.0 * coefficient;
        return BigDecimal.valueOf(liters).setScale(2, RoundingMode.HALF_UP);
    }
}
