package ru.finuniversity.advance.payment;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import ru.finuniversity.advance.payment.kafka.OutboxPublisher;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.kafka.bootstrap-servers=localhost:9999"
    }
)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class ApplicationTest {

    @MockBean JwtDecoder jwtDecoder;
    @MockBean KafkaTemplate<String, Object> kafkaTemplate;
    @MockBean OutboxPublisher outboxPublisher;

    @Test
    void contextLoads() {
    }
}
