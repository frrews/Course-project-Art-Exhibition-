package com.bsuir.exhibition.exception;

import lombok.Getter;

@Getter
public class AiProxyException extends RuntimeException {

    public AiProxyException(String message) {
        super(message);
    }

    public AiProxyException(String message, Throwable cause) {
        super(message, cause);
    }
}
