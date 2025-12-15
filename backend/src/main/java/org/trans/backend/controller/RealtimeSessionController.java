package org.trans.backend.controller;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.trans.backend.model.dto.SessionEndRequest;
import org.trans.backend.model.dto.SessionEndResponse;
import org.trans.backend.model.dto.SessionStartRequest;
import org.trans.backend.model.dto.SessionStartResponse;
import org.trans.backend.model.dto.SessionStatusResponse;
import org.trans.backend.service.SessionService;

@RestController
@RequestMapping("/api/realtime/session")
public class RealtimeSessionController {

    private static final Logger log = LoggerFactory.getLogger(RealtimeSessionController.class);

    private final SessionService sessionService;

    public RealtimeSessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping("/start")
    public ResponseEntity<SessionStartResponse> startSession(
            @Valid @RequestBody SessionStartRequest request
    ) {
        log.info("Received session start request: userId={}, sourceLang={}, targetLang={}",
                request.userId(), request.sourceLang(), request.targetLang());

        SessionStartResponse response = sessionService.startSession(request);

        log.info("Session started successfully: sessionId={}", response.sessionId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/end")
    public ResponseEntity<SessionEndResponse> endSession(
            @Valid @RequestBody SessionEndRequest request
    ) {
        log.info("Received session end request: sessionId={}", request.sessionId());

        SessionEndResponse response = sessionService.endSession(request.sessionId());

        log.info("Session ended successfully: sessionId={}, duration={}s",
                response.sessionId(), response.duration().getSeconds());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{sessionId}")
    public ResponseEntity<SessionStatusResponse> getStatus(
            @PathVariable String sessionId
    ) {
        log.debug("Received session status request: sessionId={}", sessionId);

        SessionStatusResponse response = sessionService.getStatus(sessionId);
        return ResponseEntity.ok(response);
    }
}
