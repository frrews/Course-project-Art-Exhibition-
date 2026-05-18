package com.bsuir.exhibition.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AuthorizationRequest(
        @Email
        @NotBlank
        String email,
        @NotBlank
        String password
) {
}
