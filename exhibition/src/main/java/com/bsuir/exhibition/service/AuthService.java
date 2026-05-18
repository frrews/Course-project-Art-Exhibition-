package com.bsuir.exhibition.service;

import com.bsuir.exhibition.dto.AuthResponse;
import com.bsuir.exhibition.dto.AuthorizationRequest;
import com.bsuir.exhibition.dto.RegisterRequest;
import com.bsuir.exhibition.dto.VerifyRequest;
import com.bsuir.exhibition.entity.PendingRegistration;
import com.bsuir.exhibition.entity.User;
import com.bsuir.exhibition.exception.AccountNotVerifiedException;
import com.bsuir.exhibition.exception.InvalidCredentialsException;
import com.bsuir.exhibition.exception.UserNotFoundException;
import com.bsuir.exhibition.exception.VerificationFailedException;
import com.bsuir.exhibition.repository.PendingRegistrationRepository;
import com.bsuir.exhibition.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository repository;
    private final PendingRegistrationRepository pendingRegistrationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;

    @Value("${application.registration.pending-expiration-hours:24}")
    private long pendingExpirationHours;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static String generateSixDigitOtp() {
        int n = SECURE_RANDOM.nextInt(1_000_000);
        return String.format("%06d", n);
    }

    /**
     * В таблицу {@code users} запись не попадает: только {@link PendingRegistration} и письмо с кодом.
     */
    public ResponseEntity<String> register(RegisterRequest request) {
        if (repository.findUserByEmail(request.email()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Пользователь с таким email уже зарегистрирован");
        }

        String otp = generateSixDigitOtp();
        Instant now = Instant.now();
        Instant expiresAt = now.plus(pendingExpirationHours, ChronoUnit.HOURS);

        pendingRegistrationRepository.findByEmail(request.email()).ifPresentOrElse(
                pending -> {
                    pending.setUsername(request.username());
                    pending.setPasswordHash(passwordEncoder.encode(request.password()));
                    pending.setVerificationCode(otp);
                    pending.setCreatedAt(now);
                    pending.setExpiresAt(expiresAt);
                    pendingRegistrationRepository.save(pending);
                },
                () -> {
                    PendingRegistration pending = new PendingRegistration();
                    pending.setEmail(request.email());
                    pending.setUsername(request.username());
                    pending.setPasswordHash(passwordEncoder.encode(request.password()));
                    pending.setVerificationCode(otp);
                    pending.setCreatedAt(now);
                    pending.setExpiresAt(expiresAt);
                    pendingRegistrationRepository.save(pending);
                }
        );

        try {
            emailService.sendVerificationCode(request.email(), otp);
        } catch (MailException ex) {
            log.warn("Не удалось отправить письмо на {}: {}", request.email(), ex.getMessage());
            log.warn("OTP для ручной проверки (dev): {} → {}", request.email(), otp);
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(
                    "Код отправки сохранён. Письмо не отправлено — проверьте SMTP. Код для отладки в логе сервера."
            );
        }

        return ResponseEntity.ok("На почту отправлен код подтверждения. После ввода кода аккаунт будет создан.");
    }

    @Transactional
    public ResponseEntity<String> verify(VerifyRequest request) {
        PendingRegistration pending = pendingRegistrationRepository.findByEmail(request.email())
                .orElseThrow(() -> new UserNotFoundException(
                        "Нет ожидающей регистрации для этого email. Пройдите регистрацию снова."
                ));

        if (pending.getExpiresAt().isBefore(Instant.now())) {
            pendingRegistrationRepository.delete(pending);
            throw new VerificationFailedException("Срок действия кода истёк. Зарегистрируйтесь снова.");
        }

        if (!pending.getVerificationCode().equals(request.otpCode())) {
            throw new VerificationFailedException("Неверный код подтверждения");
        }

        if (repository.findUserByEmail(request.email()).isPresent()) {
            pendingRegistrationRepository.delete(pending);
            return ResponseEntity.ok("Аккаунт уже существует. Можно войти.");
        }

        User user = new User();
        user.setEmail(pending.getEmail());
        user.setUsername(pending.getUsername());
        user.setPassword(pending.getPasswordHash());
        user.setEnabled(true);

        repository.save(user);
        pendingRegistrationRepository.delete(pending);

        return ResponseEntity.ok("Email подтверждён, аккаунт создан. Теперь можно войти.");
    }

    public ResponseEntity<AuthResponse> login(AuthorizationRequest request) {
        User userFromDB = repository.findUserByEmail(request.email())
                .orElseThrow(() -> new UserNotFoundException("Пользователь с таким email не найден!"));

        if (!userFromDB.isEnabled()) {
            throw new AccountNotVerifiedException("Аккаунт отключён. Обратитесь к администратору.");
        }

        if (passwordEncoder.matches(request.password(), userFromDB.getPassword())) {
            String token = jwtService.generateToken(userFromDB.getEmail(), userFromDB.getUsername());
            return ResponseEntity.ok(new AuthResponse(token));
        }
        throw new InvalidCredentialsException("Неверный логин или пароль");
    }
}
