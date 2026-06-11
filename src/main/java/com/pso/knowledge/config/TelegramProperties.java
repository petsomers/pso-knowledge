package com.pso.knowledge.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.List;

@ConfigurationProperties(prefix = "telegram")
public record TelegramProperties(String token, String username, List<Long> allowedUserIds) {}
