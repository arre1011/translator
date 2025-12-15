package org.trans.backend.persistence.impl;

import org.springframework.stereotype.Repository;
import org.trans.backend.model.domain.TranslationSession;
import org.trans.backend.persistence.SessionRepository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemorySessionRepository implements SessionRepository {

    private final ConcurrentHashMap<String, TranslationSession> sessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> callIdToSessionId = new ConcurrentHashMap<>();

    @Override
    public TranslationSession save(TranslationSession session) {
        sessions.put(session.getSessionId(), session);
        callIdToSessionId.put(session.getCallId(), session.getSessionId());
        return session;
    }

    @Override
    public Optional<TranslationSession> findById(String sessionId) {
        return Optional.ofNullable(sessions.get(sessionId));
    }

    @Override
    public Optional<TranslationSession> findByCallId(String callId) {
        String sessionId = callIdToSessionId.get(callId);
        if (sessionId == null) {
            return Optional.empty();
        }
        return findById(sessionId);
    }

    @Override
    public List<TranslationSession> findByUserId(String userId) {
        return sessions.values().stream()
                .filter(s -> s.getUserId().equals(userId))
                .toList();
    }

    @Override
    public List<TranslationSession> findActiveByUserId(String userId) {
        return sessions.values().stream()
                .filter(s -> s.getUserId().equals(userId))
                .filter(TranslationSession::isActive)
                .toList();
    }

    @Override
    public void delete(String sessionId) {
        TranslationSession session = sessions.remove(sessionId);
        if (session != null) {
            callIdToSessionId.remove(session.getCallId());
        }
    }
}
