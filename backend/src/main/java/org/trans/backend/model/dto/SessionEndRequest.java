package org.trans.backend.model.dto;

import jakarta.validation.constraints.NotBlank;

public record SessionEndRequest(
        @NotBlank(message = "sessionId is required")
        String sessionId
) {
}
