package com.bsuir.exhibition.dto;

import jakarta.validation.constraints.NotBlank;

public record AiChatRequest(
        @NotBlank String message,
        String context
) {
}
