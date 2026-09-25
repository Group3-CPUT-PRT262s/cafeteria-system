package com.group3.cafeteria_system.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromAddress;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendPasswordResetEmail(String toEmail, String token) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(fromAddress);
        message.setTo(toEmail);

        message.setSubject("Campus Cafeteria Account - Password Reset Code");

        message.setText(
                "Hello,\n\n" +
                        "You requested a password reset for your Campus Cafeteria account.\n\n" +
                        "Your password reset code is:\n\n" +
                        token + "\n\n" +
                        "This code will expire in 10 minutes and can only be used once.\n\n" +
                        "If you did not request a password reset, you can safely ignore this email.\n\n" +
                        "Regards,\nThe Campus Cafeteria Team"
        );

        mailSender.send(message);
    }
}