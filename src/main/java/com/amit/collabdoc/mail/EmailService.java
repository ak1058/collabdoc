package com.amit.collabdoc.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Service for sending emails asynchronously.
 */
@Service
public class EmailService {
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    // This is the 'from' address in application.yml
    @Value("${spring.mail.username}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Sends a share notification email on a background thread.
     * @param toEmail The email address of the person being invited.
     * @param ownerUsername The username of the person who is sharing.
     * @param documentTitle The title of the document.
     * @param permissionType The permission level (e.g., "VIEWER" or "EDITOR").
     */
    @Async("taskExecutor") // Uses the custom thread pool we defined
    public void sendShareNotificationEmail(String toEmail, String ownerUsername,
                                          String documentTitle, String permissionType) {
        try {
            logger.info("Sending share notification to {} for doc '{}'", toEmail, documentTitle);

            SimpleMailMessage message  = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject(ownerUsername + " shared a document with you on CollabDoc");
            message.setText("Hello,\n\n" +
                    ownerUsername + " has shared the document titled '" + documentTitle + "' with you as a " +
                    permissionType + ".\n\n" +
                    "You can access the document by logging into your CollabDoc account.\n\n" +
                    "Best regards,\n" +
                    "The CollabDoc Team");
            mailSender.send(message);
            logger.info("Successfully sent email to {}", toEmail);

        } catch (Exception e) {
            // We catch exceptions because this is @Async. If we don't, the thread will just die.
            logger.error("Failed to send email to {}: {}", toEmail, e.getMessage());
        }
    }
}
