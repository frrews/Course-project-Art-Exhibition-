package com.bsuir.exhibition.controller;

import com.bsuir.exhibition.dto.AuthResponse;
import com.bsuir.exhibition.dto.AuthorizationRequest;
import com.bsuir.exhibition.dto.RegisterRequest;
import com.bsuir.exhibition.dto.VerifyRequest;
import com.bsuir.exhibition.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Аутентификация", description = "Регистрация, подтверждение email и вход")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Регистрация", description = "Создаёт пользователя и отправляет OTP на email")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/verify")
    @Operation(summary = "Подтверждение email", description = "Проверяет OTP и активирует аккаунт")
    public ResponseEntity<String> verify(@Valid @RequestBody VerifyRequest request) {
        return authService.verify(request);
    }

    @PostMapping("/login")
    @Operation(summary = "Вход", description = "Выдаёт JWT после успешной авторизации")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthorizationRequest request) {
        return authService.login(request);
    }
}
