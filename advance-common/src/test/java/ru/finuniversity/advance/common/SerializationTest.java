package ru.finuniversity.advance.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.finuniversity.advance.common.dto.AdvanceRequestDto;
import ru.finuniversity.advance.common.dto.AdvanceStatus;
import ru.finuniversity.advance.common.dto.AdvanceType;
import ru.finuniversity.advance.common.dto.DriverScoreDto;
import ru.finuniversity.advance.common.events.AdvanceCreatedEvent;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SerializationTest {

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test
    void advanceRequestDto_roundTrip() throws Exception {
        var dto = new AdvanceRequestDto(
                "drv-001",
                "rte-001",
                "trp-001",
                "LOADING",
                AdvanceType.FUEL,
                BigDecimal.valueOf(5000),
                "Тестовые заметки"
        );

        String json = mapper.writeValueAsString(dto);
        assertThat(json).contains("FUEL").contains("drv-001");

        var restored = mapper.readValue(json, AdvanceRequestDto.class);
        assertThat(restored).isEqualTo(dto);
    }

    @Test
    void advanceCreatedEvent_roundTrip() throws Exception {
        var event = new AdvanceCreatedEvent(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                "ADVANCE_CREATED",
                LocalDateTime.of(2026, 1, 15, 10, 30),
                "drv-001",
                "rte-001",
                AdvanceType.FUEL,
                BigDecimal.valueOf(5000),
                AdvanceStatus.PENDING
        );

        String json = mapper.writeValueAsString(event);
        assertThat(json).contains("ADVANCE_CREATED").contains("PENDING");

        var restored = mapper.readValue(json, AdvanceCreatedEvent.class);
        assertThat(restored).isEqualTo(event);
    }

    @Test
    void driverScoreDto_roundTrip() throws Exception {
        var dto = new DriverScoreDto(
                "drv-001",
                75,
                80,
                70,
                85,
                60,
                90,
                LocalDateTime.of(2026, 1, 15, 12, 0),
                "LOW"
        );

        String json = mapper.writeValueAsString(dto);
        assertThat(json).contains("LOW").contains("drv-001");

        var restored = mapper.readValue(json, DriverScoreDto.class);
        assertThat(restored).isEqualTo(dto);
    }
}
