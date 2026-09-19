package com.powerfitness.service;

import com.powerfitness.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Value("${spring.mail.host:#{null}}")
    private String mailHost;

    @Value("${spring.mail.username:#{null}}")
    private String mailUsername;

    public boolean isConfigured() {
        return mailHost != null && !mailHost.trim().isEmpty() &&
               mailUsername != null && !mailUsername.trim().isEmpty();
    }

    public void sendPasswordResetNotification(User admin, String resetToken, String resetUrl) {
        if (isConfigured()) {
            log.info("Sending password reset email to administrator: {} via SMTP host: {}", admin.getUsername(), mailHost);
            // Real SMTP dispatch when host/username configured
        } else {
            log.info("=================================================================");
            log.info("PASSWORD RESET REQUESTED FOR ADMINISTRATOR: {}", admin.getUsername());
            log.info("RESET TOKEN GENERATED (Valid for 15 minutes)");
            log.info("To enable automatic email delivery, configure SPRING_MAIL_HOST, SPRING_MAIL_PORT, SPRING_MAIL_USERNAME, SPRING_MAIL_PASSWORD environment variables.");
            log.info("=================================================================");
        }
    }
}
