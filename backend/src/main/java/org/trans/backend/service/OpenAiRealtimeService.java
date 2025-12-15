package org.trans.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.trans.backend.config.OpenAiProperties;
import org.trans.backend.exception.OpenAiException;
import org.trans.backend.model.openai.OpenAiCallRequest;
import org.trans.backend.model.openai.OpenAiCallResponse;

import java.util.Map;

@Service
public class OpenAiRealtimeService {

    private static final Logger log = LoggerFactory.getLogger(OpenAiRealtimeService.class);
    private static final String INTERPRETER_PROMPT_TEMPLATE = """
            You are a professional real-time interpreter. Your task is to translate speech from %s to %s.

            CRITICAL RULES:
            1. Translate ONLY the spoken content - nothing else
            2. Do NOT add explanations, context, or commentary
            3. Do NOT ask clarifying questions
            4. Maintain the speaker's tone, register, and intent
            5. If something is unclear, translate it as best as possible
            6. Keep translations natural and conversational
            7. Respond immediately with the translation

            You will hear speech in %s. Translate it faithfully and speak the translation in %s.
            """;

    private final WebClient webClient;
    private final OpenAiProperties properties;
    private final ObjectMapper objectMapper;

    public OpenAiRealtimeService(WebClient openAiWebClient, OpenAiProperties properties, ObjectMapper objectMapper) {
        this.webClient = openAiWebClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public OpenAiCallResponse createCall(String sdpOffer, String sourceLang, String targetLang) {
        String instructions = buildInterpreterPrompt(sourceLang, targetLang);
        String normalizedSdp = normalizeSdp(sdpOffer);

        OpenAiCallRequest request = OpenAiCallRequest.create(
                properties.realtimeModel(),
                normalizedSdp,
                instructions
        );

        log.info("Creating OpenAI Realtime call: sourceLang={}, targetLang={}, model={}",
                sourceLang, targetLang, properties.realtimeModel());

        try {
            // Make the POST request to create the call
            var response = webClient.post()
                    .uri("/v1/realtime/sessions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .toEntity(String.class)
                    .block();

            if (response == null || response.getBody() == null) {
                throw new OpenAiException("Empty response from OpenAI");
            }

            // Parse the response
            JsonNode responseJson = objectMapper.readTree(response.getBody());

            // Extract call_id from response body or Location header
            String callId = null;
            if (responseJson.has("id")) {
                callId = responseJson.get("id").asText();
            }

            // Also check Location header for call_id
            HttpHeaders headers = response.getHeaders();
            String locationHeader = headers.getFirst(HttpHeaders.LOCATION);
            if (locationHeader != null && callId == null) {
                // Extract call_id from location: /v1/realtime/calls/{call_id}
                int lastSlash = locationHeader.lastIndexOf('/');
                if (lastSlash >= 0) {
                    callId = locationHeader.substring(lastSlash + 1);
                }
            }

            // Extract SDP answer
            String sdpAnswer = null;
            if (responseJson.has("sdp")) {
                sdpAnswer = responseJson.get("sdp").asText();
            } else if (responseJson.has("client_secret")) {
                // For ephemeral key flow, we need to handle differently
                // The client_secret is used by the client to connect
                sdpAnswer = responseJson.get("client_secret").get("value").asText();
            }

            if (callId == null) {
                // Generate a session ID if not provided
                callId = java.util.UUID.randomUUID().toString();
            }

            log.info("OpenAI Realtime call created: callId={}", callId);

            return new OpenAiCallResponse(callId, sdpAnswer != null ? sdpAnswer : response.getBody());

        } catch (WebClientResponseException e) {
            log.error("OpenAI API error: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new OpenAiException("OpenAI API error: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Failed to create OpenAI Realtime call", e);
            throw new OpenAiException("Failed to create call: " + e.getMessage(), e);
        }
    }

    public OpenAiCallResponse createWebRtcCall(String sdpOffer, String sourceLang, String targetLang) {
        String instructions = buildInterpreterPrompt(sourceLang, targetLang);
        String normalizedSdp = normalizeSdp(sdpOffer);

        log.info("Creating OpenAI WebRTC call: sourceLang={}, targetLang={}, model={}",
                sourceLang, targetLang, properties.realtimeModel());

        try {
            // For WebRTC, we POST to /v1/realtime with the SDP offer
            Map<String, Object> requestBody = Map.of(
                    "model", properties.realtimeModel(),
                    "sdp", normalizedSdp,
                    "instructions", instructions,
                    "voice", "alloy",
                    "input_audio_transcription", Map.of("model", "whisper-1"),
                    "turn_detection", Map.of(
                            "type", "server_vad",
                            "threshold", 0.5,
                            "prefix_padding_ms", 300,
                            "silence_duration_ms", 500
                    )
            );

            var response = webClient.post()
                    .uri("/v1/realtime?model=" + properties.realtimeModel())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .toEntity(String.class)
                    .block();

            if (response == null || response.getBody() == null) {
                throw new OpenAiException("Empty response from OpenAI");
            }

            JsonNode responseJson = objectMapper.readTree(response.getBody());

            // Extract call_id
            String callId = null;
            HttpHeaders headers = response.getHeaders();
            String locationHeader = headers.getFirst(HttpHeaders.LOCATION);
            if (locationHeader != null) {
                int lastSlash = locationHeader.lastIndexOf('/');
                if (lastSlash >= 0) {
                    callId = locationHeader.substring(lastSlash + 1);
                }
            }
            if (callId == null && responseJson.has("id")) {
                callId = responseJson.get("id").asText();
            }
            if (callId == null) {
                callId = java.util.UUID.randomUUID().toString();
            }

            // Extract SDP answer
            String sdpAnswer = responseJson.has("sdp") ? responseJson.get("sdp").asText() : response.getBody();

            log.info("OpenAI WebRTC call created: callId={}", callId);
            return new OpenAiCallResponse(callId, sdpAnswer);

        } catch (WebClientResponseException e) {
            log.error("OpenAI API error: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new OpenAiException("OpenAI API error: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Failed to create OpenAI WebRTC call", e);
            throw new OpenAiException("Failed to create call: " + e.getMessage(), e);
        }
    }

    private String buildInterpreterPrompt(String sourceLang, String targetLang) {
        String sourceLanguage = getLanguageName(sourceLang);
        String targetLanguage = getLanguageName(targetLang);
        return String.format(INTERPRETER_PROMPT_TEMPLATE,
                sourceLanguage, targetLanguage, sourceLanguage, targetLanguage);
    }

    private String getLanguageName(String code) {
        return switch (code.toLowerCase()) {
            case "en" -> "English";
            case "es" -> "Spanish";
            case "fr" -> "French";
            case "de" -> "German";
            case "it" -> "Italian";
            case "pt" -> "Portuguese";
            case "zh" -> "Chinese";
            case "ja" -> "Japanese";
            case "ko" -> "Korean";
            case "ar" -> "Arabic";
            case "ru" -> "Russian";
            case "nl" -> "Dutch";
            case "pl" -> "Polish";
            case "tr" -> "Turkish";
            case "hi" -> "Hindi";
            default -> code;
        };
    }

    private String normalizeSdp(String sdp) {
        // Ensure proper CRLF line endings
        sdp = sdp.replaceAll("\\r?\\n", "\r\n");
        // Ensure trailing CRLF
        if (!sdp.endsWith("\r\n")) {
            sdp += "\r\n";
        }
        return sdp;
    }
}
