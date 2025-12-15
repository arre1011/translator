package org.trans.backend.model.dto;

import org.trans.backend.model.domain.UsageRecord;

import java.time.Duration;

public record SessionEndResponse(
        String sessionId,
        Duration duration,
        UsageRecord finalUsage
) {
}
