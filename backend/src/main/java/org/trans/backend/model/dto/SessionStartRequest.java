package org.trans.backend.model.dto;

import jakarta.validation.constraints.NotBlank;

public record SessionStartRequest(
        @NotBlank(message = "userId is required")
        String userId,

        @NotBlank(message = "sourceLang is required")
        String sourceLang,

        @NotBlank(message = "targetLang is required")
        String targetLang,

        @NotBlank(message = "sdpOffer is required")
        String sdpOffer
) {
}
