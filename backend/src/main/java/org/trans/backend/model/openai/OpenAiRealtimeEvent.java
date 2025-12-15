package org.trans.backend.model.openai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenAiRealtimeEvent(
        String type,
        @JsonProperty("event_id")
        String eventId,
        Response response,
        Usage usage
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Response(
            String id,
            String status,
            Usage usage
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Usage(
            @JsonProperty("total_tokens")
            Integer totalTokens,
            @JsonProperty("input_tokens")
            Integer inputTokens,
            @JsonProperty("output_tokens")
            Integer outputTokens,
            @JsonProperty("input_token_details")
            TokenDetails inputTokenDetails,
            @JsonProperty("output_token_details")
            TokenDetails outputTokenDetails
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TokenDetails(
            @JsonProperty("cached_tokens")
            Integer cachedTokens,
            @JsonProperty("text_tokens")
            Integer textTokens,
            @JsonProperty("audio_tokens")
            Integer audioTokens
    ) {
    }

    public boolean isResponseDone() {
        return "response.done".equals(type);
    }

    public boolean isError() {
        return "error".equals(type);
    }

    public boolean isSessionCreated() {
        return "session.created".equals(type);
    }

    public boolean isSessionUpdated() {
        return "session.updated".equals(type);
    }
}
