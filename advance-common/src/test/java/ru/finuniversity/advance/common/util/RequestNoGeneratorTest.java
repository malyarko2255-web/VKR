package ru.finuniversity.advance.common.util;

import org.junit.jupiter.api.Test;
import java.time.Year;
import static org.assertj.core.api.Assertions.assertThat;

class RequestNoGeneratorTest {

    private final String currentYear = String.valueOf(Year.now().getValue());

    @Test
    void generate_firstSequence_formatsCorrectly() {
        String no = RequestNoGenerator.generate(1);
        assertThat(no).isEqualTo("A-" + currentYear + "-00001");
    }

    @Test
    void generate_largeSequence_noTruncation() {
        String no = RequestNoGenerator.generate(99999);
        assertThat(no).isEqualTo("A-" + currentYear + "-99999");
    }

    @Test
    void generate_midRange_paddedCorrectly() {
        String no = RequestNoGenerator.generate(42);
        assertThat(no).isEqualTo("A-" + currentYear + "-00042");
    }

    @Test
    void generate_matchesExpectedPattern() {
        String no = RequestNoGenerator.generate(1);
        assertThat(no).matches("A-\\d{4}-\\d{5}");
    }

    @Test
    void generate_yearIsCurrentYear() {
        String no = RequestNoGenerator.generate(1);
        assertThat(no).contains(currentYear);
    }
}
