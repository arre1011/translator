package org.trans.backend.model.openai;

public record OpenAiCallResponse(
        String callId,
        String sdpAnswer
) {
}
