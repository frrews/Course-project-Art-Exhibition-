package com.bsuir.exhibition.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromAddress;

    public void sendVerificationCode(String toEmail, String otpCode) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(toEmail);
        message.setSubject("Код подтверждения — Interactive Art Exhibition");
        message.setText(
                "Здравствуйте!\n\n"
                        + "Ваш код подтверждения: " + otpCode + "\n\n"
                        + "Если вы не регистрировались на сайте, проигнорируйте это письмо."
        );
        mailSender.send(message);
    }
}
