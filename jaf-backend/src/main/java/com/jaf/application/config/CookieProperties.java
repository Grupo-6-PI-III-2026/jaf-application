package com.jaf.application.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("app.security.cookie")
public record CookieProperties(boolean secure, String sameSite, Duration maxAge) {
    public CookieProperties {
        if (sameSite == null || sameSite.isBlank()) sameSite = "Lax";
        if (maxAge == null) maxAge = Duration.ofHours(1);
    }
}
