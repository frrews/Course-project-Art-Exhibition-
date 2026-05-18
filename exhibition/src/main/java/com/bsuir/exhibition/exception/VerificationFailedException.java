package com.bsuir.exhibition.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class VerificationFailedException extends RuntimeException {
    private final String message;
}
