package org.trans.backend.model.domain;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class UserQuota {
    private final String userId;
    private final AtomicInteger audioSecondsRemaining;
    private final AtomicInteger tokensRemaining;
    private final AtomicReference<LocalDate> quotaResetDate;
    private final int dailyAudioSecondsLimit;
    private final int dailyTokensLimit;

    public UserQuota(String userId, int dailyAudioSecondsLimit, int dailyTokensLimit) {
        this.userId = userId;
        this.dailyAudioSecondsLimit = dailyAudioSecondsLimit;
        this.dailyTokensLimit = dailyTokensLimit;
        this.audioSecondsRemaining = new AtomicInteger(dailyAudioSecondsLimit);
        this.tokensRemaining = new AtomicInteger(dailyTokensLimit);
        this.quotaResetDate = new AtomicReference<>(LocalDate.now());
    }

    public String getUserId() {
        return userId;
    }

    public int getAudioSecondsRemaining() {
        resetIfNewDay();
        return audioSecondsRemaining.get();
    }

    public int getTokensRemaining() {
        resetIfNewDay();
        return tokensRemaining.get();
    }

    public LocalDate getQuotaResetDate() {
        return quotaResetDate.get();
    }

    public int getDailyAudioSecondsLimit() {
        return dailyAudioSecondsLimit;
    }

    public int getDailyTokensLimit() {
        return dailyTokensLimit;
    }

    public boolean hasRemainingQuota() {
        resetIfNewDay();
        return audioSecondsRemaining.get() > 0 || tokensRemaining.get() > 0;
    }

    public boolean deductUsage(int audioSeconds, int tokens) {
        resetIfNewDay();

        // Atomic check-and-deduct for audio seconds
        int currentAudio = audioSecondsRemaining.get();
        if (audioSeconds > 0 && currentAudio < audioSeconds) {
            return false;
        }

        // Atomic check-and-deduct for tokens
        int currentTokens = tokensRemaining.get();
        if (tokens > 0 && currentTokens < tokens) {
            return false;
        }

        if (audioSeconds > 0) {
            audioSecondsRemaining.addAndGet(-audioSeconds);
        }
        if (tokens > 0) {
            tokensRemaining.addAndGet(-tokens);
        }

        return true;
    }

    private void resetIfNewDay() {
        LocalDate today = LocalDate.now();
        LocalDate lastReset = quotaResetDate.get();

        if (today.isAfter(lastReset)) {
            if (quotaResetDate.compareAndSet(lastReset, today)) {
                audioSecondsRemaining.set(dailyAudioSecondsLimit);
                tokensRemaining.set(dailyTokensLimit);
            }
        }
    }
}
