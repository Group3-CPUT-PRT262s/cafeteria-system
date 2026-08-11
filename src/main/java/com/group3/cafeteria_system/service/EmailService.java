package com.group3.cafeteria_system.service;

import org.springframework.beans.factory.annotation.Autowired;
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

    public void sendPasswordResetEmail(String toEmail,
                                       String token) {
        String resetLink =
                "http://localhost:8080/reset-password?token=" + token; // test link for our dev app

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(toEmail);
        message.setSubject("Campus Cafeteria Account — Password Reset");
        message.setText(
                "Hello,\n\n" +
                        "You requested a password reset for your " +
                        "Campus Cafeteria account.\n\n" +
                        "Use the link below to set a new password.\n" +
                        "This link expires in 30 minutes.\n\n" +
                        resetLink + "\n\n" +
                        "If you did not request this, " +
                        "you can safely ignore this email.\n\n" +
                        "Campus Cafeteria"
        ); // message will be customised better later, test for now.

        mailSender.send(message);
    }
}