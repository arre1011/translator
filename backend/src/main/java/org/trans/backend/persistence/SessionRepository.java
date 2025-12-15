package org.trans.backend.persistence;

import org.trans.backend.model.domain.TranslationSession;

import java.util.List;
import java.util.Optional;

public interface SessionRepository {

    TranslationSession save(TranslationSession session);

    Optional<TranslationSession> findById(String sessionId);

    Optional<TranslationSession> findByCallId(String callId);

    List<TranslationSession> findByUserId(String userId);

    List<TranslationSession> findActiveByUserId(String userId);

    void delete(String sessionId);
}
