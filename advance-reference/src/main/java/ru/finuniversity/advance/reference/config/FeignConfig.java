package ru.finuniversity.advance.reference.config;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(basePackages = "ru.finuniversity.advance.reference.client")
public class FeignConfig {
}
