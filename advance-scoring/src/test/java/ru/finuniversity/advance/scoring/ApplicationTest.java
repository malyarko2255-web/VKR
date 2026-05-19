package ru.finuniversity.advance.scoring;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import ru.finuniversity.advance.scoring.client.CoreClient;
import ru.finuniversity.advance.scoring.client.IdentityClient;
import ru.finuniversity.advance.scoring.client.ReferenceClient;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.kafka.bootstrap-servers=localhost:9999",
        "spring.data.redis.host=localhost",
        "spring.data.redis.port=6379"
    }
)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class ApplicationTest {

    @MockBean JwtDecoder jwtDecoder;
    @MockBean KafkaTemplate<String, Object> kafkaTemplate;
    @MockBean ReferenceClient referenceClient;
    @MockBean CoreClient coreClient;
    @MockBean IdentityClient identityClient;

    @Test
    void contextLoads() {
    }
}
