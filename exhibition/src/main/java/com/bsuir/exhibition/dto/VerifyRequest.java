package com.bsuir.exhibition.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyRequest(
        @Email @NotBlank String email,
        @NotBlank @Pattern(regexp = "^\\d{6}$", message = "Код должен состоять из 6 цифр") String otpCode
) {
}
