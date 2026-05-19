package ru.finuniversity.advance.core;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import ru.finuniversity.advance.core.client.ReferenceClient;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.kafka.bootstrap-servers=localhost:9999"
    }
)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class ApplicationTest {

    @MockBean JwtDecoder jwtDecoder;
    @MockBean KafkaTemplate<String, Object> kafkaTemplate;
    @MockBean ReferenceClient referenceClient;

    @Test
    void contextLoads() {
    }
}
