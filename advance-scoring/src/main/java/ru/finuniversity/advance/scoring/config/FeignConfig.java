package ru.finuniversity.advance.scoring.config;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(basePackages = "ru.finuniversity.advance.scoring.client")
public class FeignConfig {}
