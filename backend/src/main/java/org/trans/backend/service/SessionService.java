package org.trans.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.trans.backend.exception.QuotaExceededException;
import org.trans.backend.exception.SessionNotFoundException;
import org.trans.backend.model.domain.TranslationSession;
import org.trans.backend.model.domain.UsageRecord;
import org.trans.backend.model.dto.SessionEndResponse;
import org.trans.backend.model.dto.SessionStartRequest;
import org.trans.backend.model.dto.SessionStartResponse;
import org.trans.backend.model.dto.SessionStatusResponse;
import org.trans.backend.model.openai.OpenAiCallResponse;
import org.trans.backend.persistence.SessionRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class SessionService {

    private static final Logger log = LoggerFactory.getLogger(SessionService.class);
    private static final Duration SESSION_DURATION = Duration.ofMinutes(30);

    private final SessionRepository sessionRepository;
    private final QuotaService quotaService;
    private final OpenAiRealtimeService openAiRealtimeService;
    private final OpenAiWebSocketMonitor webSocketMonitor;

    public SessionService(
            SessionRepository sessionRepository,
            QuotaService quotaService,
            OpenAiRealtimeService openAiRealtimeService,
            OpenAiWebSocketMonitor webSocketMonitor
    ) {
        this.sessionRepository = sessionRepository;
        this.quotaService = quotaService;
        this.openAiRealtimeService = openAiRealtimeService;
        this.webSocketMonitor = webSocketMonitor;
    }

    public SessionStartResponse startSession(SessionStartRequest request) {
        String userId = request.userId();

        log.info("Starting session for user={}, sourceLang={}, targetLang={}",
                userId, request.sourceLang(), request.targetLang());

        // 1. Check quota
        if (!quotaService.hasRemainingQuota(userId)) {
            log.warn("User {} has exceeded quota", userId);
            throw new QuotaExceededException("Quota exceeded for user: " + userId);
        }

        // 2. Create call with OpenAI
        OpenAiCallResponse openAiResponse = openAiRealtimeService.createWebRtcCall(
                request.sdpOffer(),
                request.sourceLang(),
                request.targetLang()
        );

        // 3. Create and persist session
        String sessionId = UUID.randomUUID().toString();
        TranslationSession session = new TranslationSession(
                sessionId,
                openAiResponse.callId(),
                userId,
                request.sourceLang(),
                request.targetLang()
        );

        sessionRepository.save(session);

        // 4. Start WebSocket monitoring
        webSocketMonitor.startMonitoring(openAiResponse.callId(), sessionId);

        log.info("Session started: sessionId={}, callId={}, user={}",
                sessionId, openAiResponse.callId(), userId);

        return new SessionStartResponse(
                sessionId,
                openAiResponse.callId(),
                openAiResponse.sdpAnswer(),
                Instant.now().plus(SESSION_DURATION)
        );
    }

    public SessionEndResponse endSession(String sessionId) {
        log.info("Ending session: sessionId={}", sessionId);

        TranslationSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new SessionNotFoundException("Session not found: " + sessionId));

        // 1. Stop WebSocket monitoring
        webSocketMonitor.stopMonitoring(sessionId);

        // 2. Get final usage
        UsageRecord finalUsage = webSocketMonitor.getCurrentUsage(sessionId);

        // 3. Calculate duration
        session.end();
        Duration duration = Duration.between(session.getStartedAt(), session.getEndedAt());

        // 4. Deduct usage from quota
        quotaService.deductUsage(
                session.getUserId(),
                finalUsage.getTotalAudioSeconds(),
                finalUsage.getTotalTokens()
        );

        log.info("Session ended: sessionId={}, duration={}s, tokens={}, audioSeconds={}",
                sessionId, duration.getSeconds(), finalUsage.getTotalTokens(),
                finalUsage.getTotalAudioSeconds());

        return new SessionEndResponse(sessionId, duration, finalUsage);
    }

    public SessionStatusResponse getStatus(String sessionId) {
        TranslationSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new SessionNotFoundException("Session not found: " + sessionId));

        UsageRecord currentUsage = session.isActive()
                ? webSocketMonitor.getCurrentUsage(sessionId)
                : session.getUsageRecord().copy();

        return new SessionStatusResponse(
                session.getSessionId(),
                session.getCallId(),
                session.getUserId(),
                session.getSourceLang(),
                session.getTargetLang(),
                session.getStatus(),
                session.getStartedAt(),
                session.getEndedAt(),
                currentUsage
        );
    }
}
