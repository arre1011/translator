package org.trans.backend.model.dto;

import java.time.Instant;

public record SessionStartResponse(
        String sessionId,
        String callId,
        String sdpAnswer,
        Instant expiresAt
) {
}
