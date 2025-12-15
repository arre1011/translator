package org.trans.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "quota")
public record QuotaProperties(
        int defaultAudioSecondsPerDay,
        int defaultTokensPerDay
) {
}
