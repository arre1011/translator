package org.trans.backend.model.domain;

import org.trans.backend.model.dto.SessionStatusResponse.SessionStatus;

import java.time.Instant;

public class TranslationSession {
    private final String sessionId;
    private final String callId;
    private final String userId;
    private final String sourceLang;
    private final String targetLang;
    private final Instant startedAt;
    private volatile Instant endedAt;
    private volatile SessionStatus status;
    private final UsageRecord usageRecord;

    public TranslationSession(
            String sessionId,
            String callId,
            String userId,
            String sourceLang,
            String targetLang
    ) {
        this.sessionId = sessionId;
        this.callId = callId;
        this.userId = userId;
        this.sourceLang = sourceLang;
        this.targetLang = targetLang;
        this.startedAt = Instant.now();
        this.status = SessionStatus.ACTIVE;
        this.usageRecord = new UsageRecord();
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getCallId() {
        return callId;
    }

    public String getUserId() {
        return userId;
    }

    public String getSourceLang() {
        return sourceLang;
    }

    public String getTargetLang() {
        return targetLang;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    public SessionStatus getStatus() {
        return status;
    }

    public UsageRecord getUsageRecord() {
        return usageRecord;
    }

    public void end() {
        this.endedAt = Instant.now();
        this.status = SessionStatus.ENDED;
    }

    public void setError() {
        this.status = SessionStatus.ERROR;
        if (this.endedAt == null) {
            this.endedAt = Instant.now();
        }
    }

    public boolean isActive() {
        return status == SessionStatus.ACTIVE;
    }
}
