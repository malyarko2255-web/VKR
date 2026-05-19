package ru.finuniversity.advance.common.util;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.assertThat;

class FuelNormCalculatorTest {

    private static final double NORM = 28.5;

    @Test
    void baseCalculation_noCoefficients() {
        // 100 * 28.5 / 100 * 1.0 = 28.50
        BigDecimal result = FuelNormCalculator.calculate(100, NORM, "SUMMER", "PLAIN");
        assertThat(result).isEqualByComparingTo("28.50");
    }

    @Test
    void winterCoefficient_adds10Percent() {
        // 100 * 28.5 / 100 * 1.10 = 31.35
        BigDecimal result = FuelNormCalculator.calculate(100, NORM, "WINTER", "PLAIN");
        assertThat(result).isEqualByComparingTo("31.35");
    }

    @Test
    void mountainCoefficient_adds5Percent() {
        // 100 * 28.5 / 100 * 1.05 = 29.93 (rounded)
        BigDecimal result = FuelNormCalculator.calculate(100, NORM, "SUMMER", "MOUNTAIN");
        assertThat(result).isEqualByComparingTo("29.93");
    }

    @Test
    void longDistance_adds5Percent() {
        // 150 * 28.5 / 100 * 1.05 = 44.89 (rounded)
        BigDecimal result = FuelNormCalculator.calculate(150, NORM, "SUMMER", "PLAIN");
        assertThat(result).isEqualByComparingTo("44.89");
    }

    @Test
    void distanceExactly100_noLongDistanceCoefficient() {
        // 100 km is NOT > 100, so no extra coefficient
        BigDecimal withoutExtra = FuelNormCalculator.calculate(100, NORM, "SUMMER", "PLAIN");
        BigDecimal withExtra    = FuelNormCalculator.calculate(101, NORM, "SUMMER", "PLAIN");
        assertThat(withExtra).isGreaterThan(withoutExtra);
    }

    @Test
    void allCoefficients_combined() {
        // 200 * 28.5 / 100 * (1 + 0.10 + 0.05 + 0.05) = 57 * 1.20 = 68.40
        BigDecimal result = FuelNormCalculator.calculate(200, NORM, "WINTER", "MOUNTAIN");
        assertThat(result).isEqualByComparingTo("68.40");
    }

    @Test
    void caseInsensitiveSeasonAndRegion() {
        BigDecimal lower = FuelNormCalculator.calculate(100, NORM, "winter", "mountain");
        BigDecimal upper = FuelNormCalculator.calculate(100, NORM, "WINTER", "MOUNTAIN");
        assertThat(lower).isEqualByComparingTo(upper);
    }
}
