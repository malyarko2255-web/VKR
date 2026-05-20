package ru.finuniversity.advance.notification.config;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(basePackages = "ru.finuniversity.advance.notification.client")
public class FeignConfig {}
