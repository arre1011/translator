package org.trans.backend.model.openai;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OpenAiCallRequest(
        String model,
        String sdp,
        String instructions,
        String voice,
        @JsonProperty("input_audio_transcription")
        InputAudioTranscription inputAudioTranscription,
        @JsonProperty("turn_detection")
        TurnDetection turnDetection
) {
    public record InputAudioTranscription(
            String model
    ) {
    }

    public record TurnDetection(
            String type,
            Double threshold,
            @JsonProperty("prefix_padding_ms")
            Integer prefixPaddingMs,
            @JsonProperty("silence_duration_ms")
            Integer silenceDurationMs
    ) {
    }

    public static OpenAiCallRequest create(String model, String sdpOffer, String instructions) {
        return new OpenAiCallRequest(
                model,
                sdpOffer,
                instructions,
                "alloy",
                new InputAudioTranscription("whisper-1"),
                new TurnDetection("server_vad", 0.5, 300, 500)
        );
    }
}
