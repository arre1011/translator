package org.trans.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.trans.backend.config.OpenAiProperties;
import org.trans.backend.model.domain.TranslationSession;
import org.trans.backend.model.domain.UsageRecord;
import org.trans.backend.model.openai.OpenAiRealtimeEvent;
import org.trans.backend.persistence.SessionRepository;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class OpenAiWebSocketMonitor {

    private static final Logger log = LoggerFactory.getLogger(OpenAiWebSocketMonitor.class);
    private static final int MAX_RETRIES = 5;
    private static final String WS_BASE_URL = "wss://api.openai.com/v1/realtime";

    private final OpenAiProperties properties;
    private final ObjectMapper objectMapper;
    private final SessionRepository sessionRepository;
    private final Map<String, MonitoringContext> activeMonitors = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    public OpenAiWebSocketMonitor(OpenAiProperties properties, ObjectMapper objectMapper,
                                   SessionRepository sessionRepository) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.sessionRepository = sessionRepository;
    }

    public void startMonitoring(String callId, String sessionId) {
        log.info("Starting WebSocket monitoring for callId={}, sessionId={}", callId, sessionId);

        MonitoringContext context = new MonitoringContext(callId, sessionId);
        activeMonitors.put(sessionId, context);

        connectWithRetry(context, 0);
    }

    public void stopMonitoring(String sessionId) {
        log.info("Stopping WebSocket monitoring for sessionId={}", sessionId);

        MonitoringContext context = activeMonitors.remove(sessionId);
        if (context != null) {
            context.close();
        }
    }

    public UsageRecord getCurrentUsage(String sessionId) {
        MonitoringContext context = activeMonitors.get(sessionId);
        if (context != null) {
            return context.getUsageRecord().copy();
        }
        // Fall back to session repository
        return sessionRepository.findById(sessionId)
                .map(TranslationSession::getUsageRecord)
                .map(UsageRecord::copy)
                .orElse(new UsageRecord());
    }

    private void connectWithRetry(MonitoringContext context, int attempt) {
        if (attempt >= MAX_RETRIES) {
            log.error("Max retries reached for sessionId={}, giving up", context.sessionId);
            return;
        }

        if (!context.isActive()) {
            log.debug("Monitor no longer active for sessionId={}, not reconnecting", context.sessionId);
            return;
        }

        int delay = (int) Math.min(1000 * Math.pow(2, attempt), 30000);

        scheduler.schedule(() -> {
            try {
                connect(context);
            } catch (Exception e) {
                log.error("Failed to connect WebSocket for sessionId={}, attempt={}",
                        context.sessionId, attempt, e);
                connectWithRetry(context, attempt + 1);
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    private void connect(MonitoringContext context) throws Exception {
        String wsUrl = WS_BASE_URL + "?call_id=" + context.callId;
        URI uri = URI.create(wsUrl);

        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
        headers.add("Authorization", "Bearer " + properties.apiKey());
        headers.add("OpenAI-Beta", "realtime=v1");

        StandardWebSocketClient client = new StandardWebSocketClient();

        WebSocketSession session = client.execute(
                new MonitoringWebSocketHandler(context),
                headers,
                uri
        ).get(30, TimeUnit.SECONDS);

        context.setWebSocketSession(session);
        log.info("WebSocket connected for sessionId={}", context.sessionId);
    }

    @Scheduled(fixedRate = 60000)
    public void healthCheck() {
        activeMonitors.forEach((sessionId, context) -> {
            if (!context.isConnected()) {
                log.warn("WebSocket disconnected for sessionId={}, attempting reconnect", sessionId);
                connectWithRetry(context, 0);
            }
        });
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down WebSocket monitor");
        activeMonitors.values().forEach(MonitoringContext::close);
        activeMonitors.clear();
        scheduler.shutdown();
    }

    private class MonitoringWebSocketHandler extends TextWebSocketHandler {
        private final MonitoringContext context;

        public MonitoringWebSocketHandler(MonitoringContext context) {
            this.context = context;
        }

        @Override
        public void afterConnectionEstablished(WebSocketSession session) {
            log.debug("WebSocket connection established for sessionId={}", context.sessionId);
        }

        @Override
        protected void handleTextMessage(WebSocketSession session, TextMessage message) {
            try {
                String payload = message.getPayload();
                OpenAiRealtimeEvent event = objectMapper.readValue(payload, OpenAiRealtimeEvent.class);

                handleEvent(event);

            } catch (Exception e) {
                log.error("Failed to process WebSocket message for sessionId={}",
                        context.sessionId, e);
            }
        }

        private void handleEvent(OpenAiRealtimeEvent event) {
            log.debug("Received event type={} for sessionId={}", event.type(), context.sessionId);

            if (event.isResponseDone()) {
                handleResponseDone(event);
            } else if (event.isError()) {
                log.error("OpenAI error event for sessionId={}: {}", context.sessionId, event);
            } else if (event.isSessionCreated() || event.isSessionUpdated()) {
                log.info("Session event for sessionId={}: type={}", context.sessionId, event.type());
            }
        }

        private void handleResponseDone(OpenAiRealtimeEvent event) {
            OpenAiRealtimeEvent.Usage usage = null;

            // Try to get usage from response
            if (event.response() != null && event.response().usage() != null) {
                usage = event.response().usage();
            } else if (event.usage() != null) {
                usage = event.usage();
            }

            if (usage != null) {
                int inputTokens = usage.inputTokens() != null ? usage.inputTokens() : 0;
                int outputTokens = usage.outputTokens() != null ? usage.outputTokens() : 0;

                // Calculate audio seconds from audio tokens (approximately 50 tokens per second)
                int audioInputSeconds = 0;
                int audioOutputSeconds = 0;

                if (usage.inputTokenDetails() != null && usage.inputTokenDetails().audioTokens() != null) {
                    audioInputSeconds = usage.inputTokenDetails().audioTokens() / 50;
                }
                if (usage.outputTokenDetails() != null && usage.outputTokenDetails().audioTokens() != null) {
                    audioOutputSeconds = usage.outputTokenDetails().audioTokens() / 50;
                }

                // Update usage record
                UsageRecord usageRecord = context.getUsageRecord();
                usageRecord.addInputTokens(inputTokens);
                usageRecord.addOutputTokens(outputTokens);
                usageRecord.addAudioInputSeconds(audioInputSeconds);
                usageRecord.addAudioOutputSeconds(audioOutputSeconds);

                // Also update the session's usage record
                sessionRepository.findById(context.sessionId).ifPresent(session -> {
                    UsageRecord sessionUsage = session.getUsageRecord();
                    sessionUsage.addInputTokens(inputTokens);
                    sessionUsage.addOutputTokens(outputTokens);
                    sessionUsage.addAudioInputSeconds(audioInputSeconds);
                    sessionUsage.addAudioOutputSeconds(audioOutputSeconds);
                });

                log.info("Usage accumulated for sessionId={}: inputTokens={}, outputTokens={}, audioIn={}s, audioOut={}s",
                        context.sessionId, inputTokens, outputTokens, audioInputSeconds, audioOutputSeconds);
            }
        }

        @Override
        public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
            log.info("WebSocket closed for sessionId={}: status={}", context.sessionId, status);
            context.setWebSocketSession(null);

            // Attempt reconnect if still active
            if (context.isActive() && activeMonitors.containsKey(context.sessionId)) {
                connectWithRetry(context, 0);
            }
        }

        @Override
        public void handleTransportError(WebSocketSession session, Throwable exception) {
            log.error("WebSocket transport error for sessionId={}", context.sessionId, exception);
        }
    }

    private static class MonitoringContext {
        private final String callId;
        private final String sessionId;
        private final UsageRecord usageRecord = new UsageRecord();
        private final AtomicInteger retryCount = new AtomicInteger(0);
        private volatile WebSocketSession webSocketSession;
        private volatile boolean active = true;

        public MonitoringContext(String callId, String sessionId) {
            this.callId = callId;
            this.sessionId = sessionId;
        }

        public String getCallId() {
            return callId;
        }

        public String getSessionId() {
            return sessionId;
        }

        public UsageRecord getUsageRecord() {
            return usageRecord;
        }

        public boolean isActive() {
            return active;
        }

        public boolean isConnected() {
            WebSocketSession session = webSocketSession;
            return session != null && session.isOpen();
        }

        public void setWebSocketSession(WebSocketSession session) {
            this.webSocketSession = session;
            if (session != null) {
                retryCount.set(0);
            }
        }

        public void close() {
            active = false;
            WebSocketSession session = webSocketSession;
            if (session != null && session.isOpen()) {
                try {
                    session.close();
                } catch (Exception e) {
                    // Ignore close errors
                }
            }
        }
    }
}
