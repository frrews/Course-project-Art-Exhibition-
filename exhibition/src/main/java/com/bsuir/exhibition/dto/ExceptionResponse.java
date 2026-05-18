package com.bsuir.exhibition.dto;

import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

public record ExceptionResponse(
        LocalDateTime date,
        HttpStatus status,
        String name,
        String message
        ) {
}
