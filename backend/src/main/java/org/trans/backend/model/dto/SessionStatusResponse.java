package org.trans.backend.model.dto;

import org.trans.backend.model.domain.UsageRecord;

import java.time.Instant;

public record SessionStatusResponse(
        String sessionId,
        String callId,
        String userId,
        String sourceLang,
        String targetLang,
        SessionStatus status,
        Instant startedAt,
        Instant endedAt,
        UsageRecord currentUsage
) {
    public enum SessionStatus {
        ACTIVE,
        ENDED,
        ERROR
    }
}
