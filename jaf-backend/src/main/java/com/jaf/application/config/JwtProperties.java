package com.jaf.application.config;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

@Validated
@ConfigurationProperties("app.security.jwt")
public record JwtProperties(
        @NotBlank String secret,
        @NotNull @DurationUnit(ChronoUnit.SECONDS) Duration expiration) {

    @PostConstruct
    void validate() {
        if (expiration.compareTo(Duration.ofMinutes(5)) < 0
                || expiration.compareTo(Duration.ofHours(24)) > 0) {
            throw new IllegalStateException("app.security.jwt.expiration deve estar entre 5 minutos e 24 horas");
        }
        try {
            if (Base64.getDecoder().decode(secret).length < 32) {
                throw new IllegalStateException("app.security.jwt.secret deve ter pelo menos 32 bytes em Base64");
            }
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("app.security.jwt.secret deve ser Base64 válido", ex);
        }
    }
}
