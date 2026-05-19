package ru.finuniversity.advance.reference;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.kafka.core.KafkaTemplate;
import ru.finuniversity.advance.reference.client.AdvanceCoreClient;
import ru.finuniversity.advance.reference.service.KisSyncService;

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
    @MockBean AdvanceCoreClient advanceCoreClient;
    @MockBean KisSyncService kisSyncService;

    @Test
    void contextLoads() {
    }
}
