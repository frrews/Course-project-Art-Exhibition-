package com.bsuir.exhibition.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

/**
 * Данные регистрации до подтверждения email. После успешной верификации запись удаляется,
 * пользователь создаётся в {@link User}.
 */
@Data
@Entity
@Table(name = "pending_registrations")
public class PendingRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false, length = 6)
    private String verificationCode;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant expiresAt;
}
