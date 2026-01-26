package com.articurated.ordermanagement.integration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class EmailClient {
    
    public void sendEmail(String to, String subject, String body) {
        // Simulated email sending
        // In real implementation, this would integrate with SMTP server or email service
        log.info("Sending email to: {}, Subject: {}, Body: {}", to, subject, body);
        // In production, use JavaMailSender or email service API
    }

    public void sendEmailWithTemplate(String to, String template, java.util.Map<String, Object> variables) {
        // Simulated template-based email sending
        log.info("Sending templated email to: {}, Template: {}", to, template);
    }
}
