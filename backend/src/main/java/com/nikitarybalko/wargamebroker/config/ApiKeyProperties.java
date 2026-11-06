package com.nikitarybalko.wargamebroker.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "wargame.security")
public record ApiKeyProperties(
        @NotBlank String blueApiKey,
        @NotBlank String redApiKey,
        @NotBlank String adminApiKey) {
}
