package org.trans.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.trans.backend.config.QuotaProperties;
import org.trans.backend.model.domain.UserQuota;
import org.trans.backend.persistence.QuotaRepository;

@Service
public class QuotaService {

    private static final Logger log = LoggerFactory.getLogger(QuotaService.class);

    private final QuotaRepository quotaRepository;
    private final QuotaProperties quotaProperties;

    public QuotaService(QuotaRepository quotaRepository, QuotaProperties quotaProperties) {
        this.quotaRepository = quotaRepository;
        this.quotaProperties = quotaProperties;
    }

    public boolean hasRemainingQuota(String userId) {
        UserQuota quota = getOrCreateQuota(userId);
        boolean hasQuota = quota.hasRemainingQuota();
        log.debug("User {} quota check: hasRemaining={}, audioSeconds={}, tokens={}",
                userId, hasQuota, quota.getAudioSecondsRemaining(), quota.getTokensRemaining());
        return hasQuota;
    }

    public boolean deductUsage(String userId, int audioSeconds, int tokens) {
        UserQuota quota = getOrCreateQuota(userId);
        boolean success = quota.deductUsage(audioSeconds, tokens);
        if (success) {
            log.info("Deducted usage for user {}: audioSeconds={}, tokens={}. Remaining: audioSeconds={}, tokens={}",
                    userId, audioSeconds, tokens, quota.getAudioSecondsRemaining(), quota.getTokensRemaining());
        } else {
            log.warn("Failed to deduct usage for user {}: insufficient quota", userId);
        }
        return success;
    }

    public UserQuota getQuota(String userId) {
        return getOrCreateQuota(userId);
    }

    public int getRemainingAudioSeconds(String userId) {
        return getOrCreateQuota(userId).getAudioSecondsRemaining();
    }

    public int getRemainingTokens(String userId) {
        return getOrCreateQuota(userId).getTokensRemaining();
    }

    private UserQuota getOrCreateQuota(String userId) {
        return quotaRepository.getOrCreate(
                userId,
                quotaProperties.defaultAudioSecondsPerDay(),
                quotaProperties.defaultTokensPerDay()
        );
    }
}
