package com.group3.cafeteria_system.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public EmailService(JavaMailSender mailSender,
                        @Value("${app.mail.from:no-reply@cafeteria.local}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    public void sendPasswordResetEmail(String toEmail, String token) {
        send(toEmail, "Campus Cafeteria Account - Password Reset Code",
                "Hello,\n\n" +
                        "You requested a password reset for your Campus Cafeteria account.\n\n" +
                        "Your password reset code is:\n\n" +
                        token + "\n\n" +
                        "This code will expire in 10 minutes and can only be used once.\n\n" +
                        "If you did not request a password reset, you can safely ignore this email.\n\n" +
                        "Regards,\nThe Campus Cafeteria Team");
    }

    public void sendRegistrationCode(String toEmail, String code) {
        send(toEmail, "Campus Cafeteria - Verify your email",
                "Your email verification code is:\n\n"
                + code + "\n\nThis code expires in 10 minutes. "
                + "If you did not start registration, you can ignore this email.\n\n"
                + "The Campus Cafeteria Team");
    }

    private void send(String toEmail, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
    }
}