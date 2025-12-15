package org.trans.backend.persistence.impl;

import org.springframework.stereotype.Repository;
import org.trans.backend.model.domain.UserQuota;
import org.trans.backend.persistence.QuotaRepository;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryQuotaRepository implements QuotaRepository {

    private final ConcurrentHashMap<String, UserQuota> quotas = new ConcurrentHashMap<>();

    @Override
    public UserQuota save(UserQuota quota) {
        quotas.put(quota.getUserId(), quota);
        return quota;
    }

    @Override
    public Optional<UserQuota> findByUserId(String userId) {
        return Optional.ofNullable(quotas.get(userId));
    }

    @Override
    public UserQuota getOrCreate(String userId, int defaultAudioSeconds, int defaultTokens) {
        return quotas.computeIfAbsent(userId,
                id -> new UserQuota(id, defaultAudioSeconds, defaultTokens));
    }
}
