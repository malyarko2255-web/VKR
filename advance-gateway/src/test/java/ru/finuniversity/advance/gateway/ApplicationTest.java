package ru.finuniversity.advance.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.autoconfigure.exclude=" +
            "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
            "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration," +
            "org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration"
    }
)
class ApplicationTest {

    @MockBean
    ReactiveJwtDecoder reactiveJwtDecoder;

    @MockBean
    RedisRateLimiter redisRateLimiter;

    @Test
    void contextLoads() {
    }
}
