package com.group3.cafeteria_system.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

class EmailServiceTest {

    @Test
    void sendRegistrationCodeUsesMailSender() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        EmailService emailService = new EmailService(mailSender, "no-reply@cafeteria.local");

        emailService.sendRegistrationCode("student@example.com", "123456");

        ArgumentCaptor<SimpleMailMessage> messageCaptor =
                ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        SimpleMailMessage message = messageCaptor.getValue();
        assertEquals("no-reply@cafeteria.local", message.getFrom());
        assertArrayEquals(new String[] {"student@example.com"}, message.getTo());
        assertEquals("Campus Cafeteria - Verify your email", message.getSubject());
        assertTrue(message.getText().contains("123456"));
    }

    @Test
    void sendPasswordResetEmailUsesMailSender() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        EmailService emailService = new EmailService(mailSender, "no-reply@cafeteria.local");

        emailService.sendPasswordResetEmail("student@example.com", "654321");

        ArgumentCaptor<SimpleMailMessage> messageCaptor =
                ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        SimpleMailMessage message = messageCaptor.getValue();
        assertArrayEquals(new String[] {"student@example.com"}, message.getTo());
        assertEquals("Campus Cafeteria Account - Password Reset Code", message.getSubject());
        assertTrue(message.getText().contains("654321"));
    }
}
