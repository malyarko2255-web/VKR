package ru.finuniversity.advance.core.config;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(basePackages = "ru.finuniversity.advance.core.client")
public class FeignConfig {
}
