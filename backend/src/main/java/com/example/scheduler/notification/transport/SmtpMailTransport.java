package com.example.scheduler.notification.transport;

import com.example.scheduler.config.AppMailProperties;
import com.example.scheduler.exception.MailDeliveryException;
import com.example.scheduler.notification.OutgoingEmail;
import com.example.scheduler.notification.provider.ConditionalOnMailProvider;
import com.example.scheduler.notification.provider.MailProvider;
import jakarta.mail.MessagingException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

// @Component
@RequiredArgsConstructor
@ConditionalOnMailProvider(MailProvider.SMTP)
public class SmtpMailTransport implements MailTransport {
    private final JavaMailSender mailSender;
    private final AppMailProperties properties;

    @Override
    public void send(OutgoingEmail email) {
        try {
            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setFrom(properties.from(), properties.fromName());
            helper.setTo(email.to());
            helper.setSubject(email.subject());
            helper.setText(email.html(), true);
            mailSender.send(message);
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new MailDeliveryException("SMTP message could not be built: " + e.getMessage(), e);
        }
    }
}
