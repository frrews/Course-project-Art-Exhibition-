package com.bsuir.exhibition.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class AccountNotVerifiedException extends RuntimeException {
    private final String message;
}
