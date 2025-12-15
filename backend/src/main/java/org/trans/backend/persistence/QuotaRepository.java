package org.trans.backend.persistence;

import org.trans.backend.model.domain.UserQuota;

import java.util.Optional;

public interface QuotaRepository {

    UserQuota save(UserQuota quota);

    Optional<UserQuota> findByUserId(String userId);

    UserQuota getOrCreate(String userId, int defaultAudioSeconds, int defaultTokens);
}
